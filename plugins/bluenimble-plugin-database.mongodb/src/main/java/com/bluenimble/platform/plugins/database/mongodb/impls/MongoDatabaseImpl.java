/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.plugins.database.mongodb.impls;

import static com.mongodb.client.model.Filters.eq;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.BetweenFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.DefaultFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.FilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.InFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.LikeFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.NilFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.RegexFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.TextFilterAppender;
import com.bluenimble.platform.query.Caching.Target;
import com.bluenimble.platform.query.CompiledQuery;
import com.bluenimble.platform.query.Condition;
import com.bluenimble.platform.query.Filter;
import com.bluenimble.platform.query.OrderBy;
import com.bluenimble.platform.query.OrderBy.Direction;
import com.bluenimble.platform.query.OrderByField;
import com.bluenimble.platform.query.Query;
import com.bluenimble.platform.query.Query.Conjunction;
import com.bluenimble.platform.query.Query.Operator;
import com.bluenimble.platform.query.Select;
import com.bluenimble.platform.query.Where;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * 
 * TODO:
 * 	- handle and / or in queries
 * 	- search on relations 
 * 	- applyBindings
 * 	- near and within operators
 * 
 **/
public class MongoDatabaseImpl implements Database {

	private static final long serialVersionUID = 3547537996525908902L;
	
	public static final String IdPostfix = Lang.DOT + Database.Fields.Id;

	interface Describe {
		String Size 		= "size";
		String Entities 	= "entities";
			String Name 	= "name";
			String Count 	= "count";
	}
	
	interface SpiDescribe {
		String Size 		= "size";
		String DbStats 		= "dbStats";
		String DbSize 		= "dbSize";
		String CollStats 	= "collStats";
	}
	
	interface Tokens {
		String Or 			= "$or";
		String And 			= "$and";
		String Sort 		= "$sort";
		String Skip 		= "$skip";
		String Limit 		= "$limit";
		String Set 			= "$set";
		String Unset 		= "$unset";
		String WiredTiger 	= "wiredTiger";
		String IndexDetails = "indexDetails";
	}
	
	private static final Map<Conjunction, String> MDBConjunctions = new HashMap<Conjunction, String> ();
	static {
		MDBConjunctions.put (Conjunction.or, Lang.DOLLAR + Conjunction.or.name ());
		MDBConjunctions.put (Conjunction.and, Lang.DOLLAR + Conjunction.and.name ());
	}
	
	private static final FilterAppender DefaultFilterAppender = new DefaultFilterAppender ();
	
	private static final Map<Operator, FilterAppender> FilterAppenders = new HashMap<Operator, FilterAppender> ();
	static {
		FilterAppenders.put (Operator.like, new LikeFilterAppender ());
		FilterAppenders.put (Operator.nlike, new LikeFilterAppender ());
		FilterAppenders.put (Operator.btw, new BetweenFilterAppender ());
		FilterAppenders.put (Operator.nbtw, new BetweenFilterAppender ());
		FilterAppenders.put (Operator.nil, new NilFilterAppender ());
		FilterAppenders.put (Operator.nnil, new NilFilterAppender ());
		FilterAppenders.put (Operator.in, new InFilterAppender ());
		FilterAppenders.put (Operator.nin, new InFilterAppender ());
		FilterAppenders.put (Operator.regex, new RegexFilterAppender ());
		FilterAppenders.put (Operator.ftq, new TextFilterAppender ());
	}
	
	private Map<String, BasicDBObject> QueriesCache = new ConcurrentHashMap<String, BasicDBObject> ();
	
	private MongoClient			client;
	private MongoDatabase		db;
	
	ClientSession 				session;
	
	private Tracer				tracer;
	private boolean 			caseSensitive;
	private boolean 			allowProprietaryAccess;
	private boolean				isTransaction;
	private boolean 			compileQuerySupported;
	
	public MongoDatabaseImpl (MongoClient client, String databaseName, Tracer tracer, boolean caseSensitive, boolean allowProprietaryAccess) {
		this.client					= client;
		this.db 					= client.getDatabase (databaseName);
		this.tracer 				= tracer;
		this.allowProprietaryAccess = allowProprietaryAccess;
	}
	
	@Override
	public long size () throws DatabaseException {
		Document dbStats = null;
		if (session == null) {
			dbStats = db.runCommand (new Document (SpiDescribe.DbStats, 1));
		} else {
			dbStats = db.runCommand (session, new Document (SpiDescribe.DbStats, 1));
		}
		return dbStats.getLong (SpiDescribe.DbSize);
	}

	@Override
	public void createEntity (String entity, Field... fields) throws DatabaseException {
		checkNotNull (entity);
		if (session == null) {
			db.createCollection (entity (entity));
		} else {
			db.createCollection (session, entity (entity));
		}
	}
	
	@Override
	public DatabaseObject create (String entity) throws DatabaseException {
		return new DatabaseObjectImpl (this, entity (entity));
	}

	@Override
	public List<DatabaseObject> createList () {
		return new DatabaseObjectList<DatabaseObject> (this);
	}

	@Override
	public MongoDatabaseImpl trx () {
		if (!isTransaction) {
			if (session == null) {
				tracer.log (Tracer.Level.Info, "Start Session");
				session = client.startSession ();
			}
			tracer.log (Tracer.Level.Info, "Start Transaction");
			session.startTransaction (TransactionOptions.builder ().writeConcern (WriteConcern.ACKNOWLEDGED).build ());
		}
		isTransaction = true;
		return this;
	}
	
	@Override
	public MongoDatabaseImpl commit () throws DatabaseException {
		if (!isTransaction) {
			return this;
		}
		tracer.log (Tracer.Level.Info, "Commit Transaction {0}", db);
		session.commitTransaction ();
		isTransaction = false;
		return this;
	}

	@Override
	public MongoDatabaseImpl rollback () throws DatabaseException {
		if (!isTransaction) {
			return this;
		}
		if (session == null) {
			return this;
		}
		tracer.log (Tracer.Level.Info, "Rollback Transaction");
		session.abortTransaction ();
		return this;
	}

	@Override
	public int increment (DatabaseObject object, String field, int value) throws DatabaseException {
		throw new UnsupportedOperationException ("MongoDB - increment not supported");
	}

	@Override
	public DatabaseObject get (String entity, Object id) throws DatabaseException {
		
		entity = entity (entity);
		
		Document document = _get (entity, id);
	    if (document == null) {
		    return null;
	    }
		
		return new DatabaseObjectImpl (this, entity, document);

	}
	
	Document _get (String entity, Object id) {
		if (id == null) {
			return null;
		}
		
		MongoCollection<Document> collection = db.getCollection (entity);
		if (collection == null) {
			return null;
		}
		
		Object _id = id;
		if (!(id instanceof ObjectId) && ObjectId.isValid (String.valueOf (id))) {
			_id = new ObjectId (String.valueOf (id));
		}
		
		if (session == null) {
		    return collection.find (eq (DatabaseObjectImpl.ObjectIdKey, _id)).first ();
		} else {
		    return collection.find (session, eq (DatabaseObjectImpl.ObjectIdKey, _id)).first ();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<DatabaseObject> find (String entity, Query query, Visitor visitor) throws DatabaseException {
		
		entity = entity (Lang.isNullOrEmpty (entity) ? query.entity () : entity);
		
		MongoIterable<Document> result;
		try {
			result = (MongoIterable<Document>)_query (entity, Query.Construct.select, query);
		} catch (Exception e) {
			throw new DatabaseException (e.getMessage (), e);
		}
		if (result == null) {
			return null;
		}
		
		return toList (entity, result, visitor, false);
	}

	@Override
	public DatabaseObject findOne (String entity, Query query) throws DatabaseException {
		
		// force count to 1
		query.count (1);
		
		List<DatabaseObject> result = find (entity, query, null);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);
	}

	@Override
	public int delete (String entity, Object id) throws DatabaseException {
		
		checkNotNull (entity);
		
		entity = entity (entity);
		
		if (id == null) {
			throw new DatabaseException ("can't delete object (missing object id)");
		}
		
		MongoCollection<Document> collection = db.getCollection (entity);
		if (collection == null) {
			return 0;
		}
		
		DeleteResult result = null;
		
		if (session == null) {
			result = collection.deleteOne (eq (DatabaseObjectImpl.ObjectIdKey, new ObjectId (String.valueOf (id))));
		} else {
			result = collection.deleteOne (session, eq (DatabaseObjectImpl.ObjectIdKey, new ObjectId (String.valueOf (id))));
		}
		
		return (int)result.getDeletedCount ();
		
	}

	@Override
	public void clear (String entity) throws DatabaseException {
		checkNotNull (entity);
		
		entity = entity (entity);
		
		MongoCollection<Document> collection = db.getCollection (entity);
		if (collection == null) {
			return;
		}
		
		if (session == null) {
			collection.deleteMany (new Document ());
		} else {
			collection.deleteMany (session, new Document ());
		}
	}

	@Override
	public long count (String entity) throws DatabaseException {
		checkNotNull (entity);
		
		entity = entity (entity);
		
		MongoCollection<Document> collection = db.getCollection (entity);
		if (collection == null) {
			return 0;
		}
		
		if (session == null) {
			return collection.countDocuments ();
		} else {
			return collection.countDocuments (session);
		}
	}

	@Override
	public int delete (String entity, Query query) throws DatabaseException {
		
		if (query == null) {
			return 0;
		}
		
		Object result = _query (entity, Query.Construct.delete, query);
		if (result == null) {
			return 0;
		}
		
		return (Integer)result;
	}

	@Override
	public void finish (boolean withError) {
		try {
			if (withError) {
				rollback ();
			} else {
				commit ();
			}
		} catch (DatabaseException ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	}

	@Override
	public void recycle () {
		if (session == null) {
			return;
		}
		
		tracer.log (Tracer.Level.Info, "Recycling database {0}", db);
		session.close ();
	}

	@Override
	public JsonObject describe () {
		
		JsonObject describe = new JsonObject ();
		
		describe.set (Describe.Size, 0);
		
		MongoIterable<String> collections = null;
				
		if (session == null) {
			collections = db.listCollectionNames ();
		} else {
			collections = db.listCollectionNames (session);
		}
		
		if (collections == null) {
			return describe;
		}
		
		long size = 0;
		
		JsonArray aEntities = new JsonArray ();
		describe.set (Describe.Entities, aEntities);
		
		for (String collection : collections) {
			JsonObject oEntity = new JsonObject ();
			oEntity.set (Describe.Name, collection);
			
			Document callStats = null;
			if (session == null) {
				callStats = db.runCommand (new Document (SpiDescribe.CollStats, collection));
			} else {
				callStats = db.runCommand (session, new Document (SpiDescribe.CollStats, collection));
			}
			
			// clean 
			callStats.remove (Tokens.WiredTiger);
			callStats.remove (Tokens.IndexDetails);
			
			oEntity.putAll (callStats);
			aEntities.add (oEntity);
			
			size += Json.getLong (oEntity, SpiDescribe.CollStats, 0);
		}
		
		describe.set (Describe.Size, size);
		
		return describe;
	}
	
	@Override
	public boolean isEntity (Object value) throws DatabaseException {
		if (value == null) {
			return false;
		}
		return DatabaseObject.class.isAssignableFrom (value.getClass ());
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonObject bulk (JsonObject data) throws DatabaseException {
		if (Json.isNullOrEmpty (data)) {
			throw new DatabaseException ("no data to be added to database");
		}
		
		Date now = new Date ();
		
		JsonObject result = new JsonObject ();
		
		int total = 0;
		
		Iterator<String> entities = data.keys ();
		while (entities.hasNext ()) {
			String entity = entities.next ();
			JsonArray array = Json.getArray (data, entity);
			if (array == null || array.isEmpty ()) {
				continue;
			}
			List<Document> records = new ArrayList<Document> ();
			for (int i = 0; i < array.count (); i++) {
				JsonObject object = (JsonObject)array.get (i);
				boolean add = prepareObject (object, now);
				if (!add) {
					continue;
				}
				Document record = new Document ();

				Object id = object.get (Database.Fields.Id);
				if (id != null) {
					if (id instanceof String && ObjectId.isValid ((String)id)) {
						record.append (DatabaseObjectImpl.ObjectIdKey, new ObjectId ((String)id));
					} else {
						record.append (DatabaseObjectImpl.ObjectIdKey, id);
					}
					object.remove (Database.Fields.Id);
				}

				record.putAll (object);
				records.add (record);
			}
			total += array.count ();
			
			result.set (entity, array.count ());
			
			entity = entity (entity);

			if (this.session == null) {
				this.getInternal ().getCollection (entity).insertMany (records);
			} else {
				this.getInternal ().getCollection (entity).insertMany (this.session, records);
			}
		}
		
		return (JsonObject)result.set (Database.Fields.Total, total);
		
	}

	@Override
	public DatabaseObject popOne (String entity, Query query) throws DatabaseException {
		
		entity = entity (entity);
		
		DatabaseObject dbo = findOne (entity, query);
		
		if (dbo != null) {
			dbo.delete ();
		}
		
		return dbo;
	}

	@Override
	public List<DatabaseObject> pop (String entity, Query query, Visitor visitor) throws DatabaseException {
		List<DatabaseObject> list = find (entity, query, visitor);
		delete (entity, query);
		return list;
	}
	
	@Override
	public long update (String entity, Query query, JsonObject data) throws DatabaseException {
		if (Json.isNullOrEmpty (data)) {
			return 0;
		}
		
		Object set 		= data.get (Keywords.Set);
		Object unset 	= data.get (Keywords.Unset);
		if (set == null && unset == null) {
			data = (JsonObject)new JsonObject ().set (Tokens.Set, data);
		} else {
			if (set != null) {
				data.set (Tokens.Set, set);
				data.remove (Keywords.Set);
			}
			if (unset != null) {
				data.set (Tokens.Unset, unset);
				data.remove (Keywords.Unset);
			}
		}
		
		entity = entity (Lang.isNullOrEmpty (entity) ? query.entity () : entity);
		
		MongoCollection<Document> collection = db.getCollection (entity);
		if (collection == null) {
			return 0;
		}
		
		BasicDBObject object = new BasicDBObject ();
		object.putAll (data);
		
		UpdateResult result;
		
		if (session == null) {
			result = collection.updateMany (
				createQuery (entity, Query.Construct.update, query), 
				object
			);
		} else {
			result = collection.updateMany (
				session,
				createQuery (entity, Query.Construct.update, query), 
				object
			);
		}
		
		return result.getModifiedCount ();
	}

	private List<DatabaseObject> toList (String entity, MongoIterable<Document> documents, Visitor visitor, boolean partial) {
		
		if (visitor == null) {
			List<Document> items = documents.into (new ArrayList<Document> ());
			if (items == null || items.isEmpty ()) {
				return null;
			}
			return new DatabaseObjectList<DatabaseObject> (this, items, entity, partial);
		}
		
		DatabaseObjectImpl dbo = null;
		if (visitor.optimize ()) {
			dbo = new DatabaseObjectImpl (this, entity);
		}
		
		for (Document document : documents) {
			if (visitor.optimize ()) {
				dbo.document (document);
			} else {
				dbo = new DatabaseObjectImpl (this, entity, document);
				dbo.partial = partial;
			}
			boolean cancel = visitor.onRecord (dbo);
			if (cancel) {
				return null;
			}
		}
		return null;
	}

	private Object _query (String entity, Query.Construct construct, final Query query) throws DatabaseException {
		// check if it's an aggregation
		Object qNative = query.getNative ();
		if (qNative != null && qNative instanceof JsonArray && construct.equals (Query.Construct.select)) {
			// create the pipeline
			JsonArray aNative = (JsonArray)qNative;
			List<BasicDBObject> pipeline = new ArrayList<BasicDBObject>();
			for (int i = 0; i < aNative.count (); i++) {
				pipeline.add (new BasicDBObject ((JsonObject)aNative.get (i)));
			}
			if (session == null) {
				return db.getCollection (entity).aggregate (pipeline);
			} else {
				return db.getCollection (entity).aggregate (session, pipeline);
			}
		}
		
		BasicDBObject mQuery = createQuery (entity, construct, query);
		if (mQuery == null) {
			return null;
		}

		if (Query.Construct.select.equals (construct)) {
			FindIterable<Document> cursor = null;
			if (session == null) {
				cursor = db.getCollection (entity).find (mQuery);
			} else {
				cursor = db.getCollection (entity).find (session, mQuery);
			}
			
			Select select = query.select ();
			if (select != null && select.count () > 0) {
				String [] pFields = new String [select.count ()];
				for (int i = 0; i < select.count (); i++) {
					pFields [i] = select.get (i);
				}
				cursor.projection (Projections.include (pFields));
			}
			
			// start / skip
			if (query.start () > 0) {
				cursor.skip (query.start ());
			}
			
			// count / limit
			if (query.count () > 0) {
				cursor.limit (query.count ());
			}
			
	        // orderBy
			OrderBy orderBy = query.orderBy ();
			if (orderBy != null && orderBy.count () > 0) {
				List<Bson> sorts = new ArrayList<Bson> ();
				Iterator<String> oFields = orderBy.fields ();
				while (oFields.hasNext ()) {
					String field = oFields.next ();
					OrderByField of = orderBy.get (field);
					Bson sort = null;
					if (of.direction ().equals (Direction.asc)) {
						sort = Sorts.ascending (field);
					} else {
						sort = Sorts.descending (field);
					}
					sorts.add (sort);
				}
				cursor.sort (Sorts.orderBy (sorts));
			}
			
			return cursor;
		} else if (Query.Construct.delete.equals (construct)) {
			if (session == null) {
				return db.getCollection (entity).deleteMany (mQuery);
			} else {
				return db.getCollection (entity).deleteMany (session, mQuery);
			}
		}
		
		return null;
		
	}
	
	private BasicDBObject createQuery (String entity, Query.Construct construct, Query query) throws DatabaseException {
		
		checkNotNull (entity);
		
		if (query == null) {
			return null;
		}
		
		boolean queryHasEntity = true;
		
		if (Lang.isNullOrEmpty (query.entity ())) {
			queryHasEntity = false;
		}
		
		tracer.log (Tracer.Level.Info, "Query Entity {0}", entity);
		
		String cacheKey = construct.name () + query.name ();
		
		BasicDBObject 		mQuery 		= null;
		Map<String, Object> bindings 	= query.bindings ();
		
		boolean cacheable = compileQuerySupported && queryHasEntity && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ());
		
		if (cacheable) {
			mQuery 		= (BasicDBObject)QueriesCache.get (cacheKey);
			tracer.log (Tracer.Level.Info, "Query meta loaded from cache {0}", mQuery);
		} 
		
		if (mQuery == null) {
			CompiledQuery cQuery = compile (entity, construct, query);
			
			mQuery 		= (BasicDBObject)cQuery.query ();
			bindings	= cQuery.bindings 	();
			
			if (cacheable && mQuery != null) {
				QueriesCache.put (cacheKey, mQuery);
				tracer.log (Tracer.Level.Debug, "Query meta stored in cache {0}", mQuery);
			} 
		}
		
		mQuery = cacheable ? applyBindings (mQuery, bindings) : mQuery;
		
		tracer.log (Tracer.Level.Info, "       Query {0}", mQuery);
		tracer.log (Tracer.Level.Info, "    Bindings {0}", bindings);
		
		return mQuery;
		
	}
	
	private BasicDBObject applyBindings (BasicDBObject mQuery, Map<String, Object> bindings) {
		if (mQuery == null || bindings == null || bindings.isEmpty ()) {
			return mQuery;
		}
		
		mQuery = (BasicDBObject)mQuery.copy ();
		
		// TODO: apply bindings
		// compileQuerySupported
		
		return mQuery;
	}
	
	// REF: https://docs.mongodb.com/manual/reference/method/db.collection.find/
	private CompiledQuery compile (String entity, Query.Construct construct, final Query query) {
		
		BasicDBObject mq = null;
		if (query.getNative () != null) {
			mq  = new BasicDBObject ((JsonObject)query.getNative ());
		} else {
			mq = new BasicDBObject ();
		}
		
		BasicDBObject fmq = mq;
		
		CompiledQuery cQuery = new CompiledQuery () {
			@Override
			public Object query () {
				return fmq;
			}
			
			@Override
			public Map<String, Object> bindings () {
				return query.bindings ();
			}
		};
		
		if (query.getNative () != null) {
			return cQuery;
		}
		
		// where
		Where where = query.where ();
		
		// Filters
		if (where != null) {
			applyFilter (where, fmq, query.bindings ());
		}
		
		// Aggregates / ?

		return cQuery;
		
	}
	
	private void applyFilter (Filter filter, BasicDBObject mq, Map<String, Object> bindings) {
		if (filter == null || filter.count () == 0) {
			return;
		}
		
		Iterator<String> fields = filter.conditions ();
		while (fields.hasNext ()) {
			String field = fields.next ();
			Object condOrFilter = filter.get (field);
			if (Condition.class.isAssignableFrom (condOrFilter.getClass ())) {
				applyCondition (mq, (Condition)condOrFilter, bindings);
			} else if (List.class.isAssignableFrom (condOrFilter.getClass ())) {
				// check if it's 'and' or 'or'
				Conjunction subConjunction = null;
				try {
					subConjunction = Conjunction.valueOf (field);
				} catch (Exception ex) {
					// ignore
					continue;
				}
				
				BasicDBList mdbList = new BasicDBList ();
				mq.put (MDBConjunctions.get (subConjunction), mdbList);
				
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>)condOrFilter;
				for (int i = 0; i < list.size (); i++) {
					Object subCondOrFilter = list.get (i);
					if (!Filter.class.isAssignableFrom (subCondOrFilter.getClass ())) {
						continue;
					}
					BasicDBObject cObject = new BasicDBObject ();
					mdbList.add (cObject);
					applyFilter ((Filter)subCondOrFilter, cObject, bindings);
				}
			}
		}
	}
	
	private void applyCondition (BasicDBObject mq, Condition condition, Map<String, Object> bindings) {
		String field = condition.field ();
		
		boolean idIsUserField = false;
		// ignore id replacement
		if (field.startsWith (Lang.XMARK)) {
			field = field.substring (1);
			idIsUserField = true;
		}
		
		BasicDBObject criteria = (BasicDBObject)mq.get (field);
		
		FilterAppender fa = FilterAppenders.get (condition.operator ());
		if (fa == null) {
			fa = DefaultFilterAppender;
		}
		
		// check if it's in bindings
		Object value = condition.value ();
		if (value != null && value instanceof String) {
			String sValue = (String)value;
			if (sValue.startsWith (Lang.COLON)) {
				Object candidateValue = bindings.get (sValue.substring (1));
				if (candidateValue != null) {
					value = candidateValue;
				}
			}
		}
		
		boolean newlyCreated = false;
		if (criteria == null) {
			newlyCreated = true;
			criteria = new BasicDBObject ();
			mq.put (field, criteria);
		}
		
		if (value instanceof LocalDateTime) {
			LocalDateTime ldt = ((LocalDateTime)value);
			value = Date.from (ldt.atZone (ZoneOffset.UTC).toInstant ());
		}
		
		if ((field.equals (Database.Fields.Id) || field.endsWith (IdPostfix)) && 
				!idIsUserField &&
				ObjectId.isValid (String.valueOf (value))) {
			value = new ObjectId (String.valueOf (value));
		}
		
		BasicDBObject filter = fa.append (condition, criteria, value);
		if (filter != null) {
			mq.putAll ((Map<String, Object>)filter);
			if (newlyCreated) {
				mq.remove (field);
			}
		} else if (field.endsWith (IdPostfix) && !idIsUserField) {
			mq.put (
				field.substring (0, field.length () - IdPostfix.length ()) 
				+ Lang.DOT + DatabaseObjectImpl.ObjectIdKey, 
				criteria
			);
			mq.remove (field);
		} else if (field.equals (Database.Fields.Id) && !idIsUserField) {
			mq.put (DatabaseObjectImpl.ObjectIdKey, criteria);
			mq.remove (field);
		}
	}
	
	private void checkNotNull (String entity) throws DatabaseException {
		if (Lang.isNullOrEmpty (entity)) {
			throw new DatabaseException ("entity name is null");
		}
	}

	public MongoDatabase getInternal () {
		return db;
	}

	@Override
	public Object proprietary (String name) {
		if (!allowProprietaryAccess || !Proprietary.Database.equalsIgnoreCase (name)) {
			return null;
		}
		return getInternal ();
	}
	
	public String entity (String entity) {
		if (Lang.isNullOrEmpty (entity)) {
			return entity;
		}
		if (!caseSensitive) {
			entity = entity.toUpperCase ();
		}
		return entity;
	}

	@Override
	public JsonObject describeEntity (String enity) throws DatabaseException {
		JsonObject oEntity = new JsonObject ();
		oEntity.set (Describe.Name, enity);
		
		Document callStats = null;
		if (session == null) {
			callStats = db.runCommand (new Document (SpiDescribe.CollStats, enity));
		} else {
			callStats = db.runCommand (session, new Document (SpiDescribe.CollStats, enity));
		}
		
		// clean 
		callStats.remove (Tokens.WiredTiger);
		callStats.remove (Tokens.IndexDetails);
		
		oEntity.putAll (callStats);
		
		return oEntity;
		
	}
	
	private boolean prepareObject (JsonObject object, Date now) {
		if (Json.isNullOrEmpty (object)) {
			return false;
		}
		List<String> refs = null;
		Iterator<String> keys = object.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			if (!key.startsWith (Lang.DOLLAR)) {
				continue;
			}
			JsonObject ref = Json.getObject (object, key);
			String entity = entity (Json.getString (ref, Database.Fields.Entity));
			String id = Json.getString (ref, Database.Fields.Id);
			if (Lang.isNullOrEmpty (entity) || Lang.isNullOrEmpty (id)) {
				continue;
			}
			Object objectId = id;
			if (ObjectId.isValid (id)) {
				objectId = new ObjectId (id);
			}
			ref.set (DatabaseObjectImpl.ObjectIdKey, objectId);
			ref.set (DatabaseObjectImpl.ObjectEntityKey, entity);
			
			// remove fields
			ref.remove (Database.Fields.Entity);
			ref.remove (Database.Fields.Id);
			if (refs == null) {
				refs = new ArrayList<String> ();
			}
			refs.add (key);
		}
		if (!object.containsKey (Database.Fields.Timestamp)) {
			object.set (Database.Fields.Timestamp, now);
		}
		if (refs == null) {
			return true;
		}
		for (String ref : refs) {
			object.set (ref.substring (1), object.get (ref));
			object.remove (ref);
		}
		return true;
	}
	
}

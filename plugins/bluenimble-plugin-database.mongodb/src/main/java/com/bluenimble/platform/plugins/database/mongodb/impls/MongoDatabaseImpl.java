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

import java.util.ArrayList;
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
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.Caching.Target;
import com.bluenimble.platform.db.query.CompiledQuery;
import com.bluenimble.platform.db.query.Condition;
import com.bluenimble.platform.db.query.OrderBy;
import com.bluenimble.platform.db.query.OrderBy.Direction;
import com.bluenimble.platform.db.query.OrderByField;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.Query.Operator;
import com.bluenimble.platform.db.query.Select;
import com.bluenimble.platform.db.query.Where;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.BetweenFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.DefaultFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.FilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.LikeFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.NilFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.RegexFilterAppender;
import com.bluenimble.platform.plugins.database.mongodb.impls.filters.TextFilterAppender;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;

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
	
	interface Describe {
		String Size 		= "size";
		String Entities 	= "entities";
			String Name 	= "name";
			String Count 	= "count";
	}
	
	interface SpiDescribe {
		String Size 		= "size";
		String CollStats 	= "collStats";
	}
	
	interface Tokens {
		String Sort 	= "$sort";
		String Skip 	= "$skip";
		String Limit 	= "$limit";
	}
	
	interface Proprietary {
		String Database = "database";
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
		FilterAppenders.put (Operator.regex, new RegexFilterAppender ());
		FilterAppenders.put (Operator.ftq, new TextFilterAppender ());
		//FilterAppenders.put (Operator.near, "");
		//FilterAppenders.put (Operator.within, "");
	}
	
	private Map<String, BasicDBObject> QueriesCache = new ConcurrentHashMap<String, BasicDBObject> ();
	
	private MongoDatabase		db;
	private Tracer				tracer;
	private boolean 			allowProprietaryAccess;
	
	public MongoDatabaseImpl (MongoDatabase db, Tracer tracer, boolean allowProprietaryAccess) {
		this.db 					= db;
		this.tracer 				= tracer;
		this.allowProprietaryAccess = allowProprietaryAccess;
	}

	@Override
	public DatabaseObject create (String entity) throws DatabaseException {
		return new DatabaseObjectImpl (this, entity);
	}

	@Override
	public void trx () {
		// Not Supported
	}
	
	@Override
	public void commit () throws DatabaseException {
		// Not Supported
	}

	@Override
	public void rollback () throws DatabaseException {
		// Not Supported
	}

	@Override
	public int increment (DatabaseObject object, String field, int value) throws DatabaseException {
		throw new UnsupportedOperationException ("MongoDB - increment not supported");
	}

	@Override
	public DatabaseObject get (String entity, Object id) throws DatabaseException {
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
		if (ObjectId.isValid (String.valueOf (id))) {
			_id = new ObjectId (String.valueOf (id));
		}
		
	    return collection.find (eq (DatabaseObjectImpl.ObjectIdKey, _id)).first ();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<DatabaseObject> find (String entity, Query query, Visitor visitor) throws DatabaseException {
		
		FindIterable<Document> result;
		try {
			result = (FindIterable<Document>)_query (entity, Query.Construct.select, query);
		} catch (Exception e) {
			throw new DatabaseException (e.getMessage (), e);
		}
		if (result == null) {
			return null;
		}
		
		return toList (entity, result, visitor, query.select () != null);
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
		
		if (id == null) {
			throw new DatabaseException ("can't delete object (missing object id)");
		}
		
		MongoCollection<Document> collection = db.getCollection (entity);
		if (collection == null) {
			return 0;
		}
		
		DeleteResult result = collection.deleteOne (eq (DatabaseObjectImpl.ObjectIdKey, new ObjectId (String.valueOf (id))));
		
		return (int)result.getDeletedCount ();
		
	}

	@Override
	public void clear (String entity) throws DatabaseException {
		checkNotNull (entity);
		
		MongoCollection<Document> collection = db.getCollection (entity);
		if (collection == null) {
			return;
		}
		
		collection.deleteMany (new Document ());
	}

	@Override
	public long count (String entity) throws DatabaseException {
		
		checkNotNull (entity);
		
		MongoCollection<Document> collection = db.getCollection (entity);
		if (collection == null) {
			return 0;
		}
		
		return collection.count ();
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
	public void recycle () {
	}

	@Override
	public JsonObject describe () {
		
		JsonObject describe = new JsonObject ();
		
		describe.set (Describe.Size, 0);
		
		MongoIterable<String> collections = db.listCollectionNames ();
		
		if (collections == null) {
			return describe;
		}
		
		long size = 0;
		
		JsonArray aEntities = new JsonArray ();
		describe.set (Describe.Entities, aEntities);
		
		for (String collection : collections) {
			JsonObject oEntity = new JsonObject ();
			oEntity.set (Describe.Name, collection);
			oEntity.putAll (db.runCommand (new Document (SpiDescribe.CollStats, collection)));
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

	@Override
	public JsonObject bulk (JsonObject data) throws DatabaseException {
		
		JsonObject result = (JsonObject)new JsonObject ().set (Database.Fields.Total, 0);
		
		if (data == null || data.isEmpty ()) {
			return result;
		}
			
		// TODO
		throw new UnsupportedOperationException ("MongoDB - bulk not supported");
		
		// return result;
		
	}

	@Override
	public DatabaseObject popOne (String entity, Query query) throws DatabaseException {
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
	private List<DatabaseObject> toList (String entity, FindIterable<Document> documents, Visitor visitor, boolean partial) {
		
		if (visitor == null) {
			return new DatabaseObjectList<DatabaseObject> (this, documents.into (new ArrayList<Document> ()), entity, partial);
		}
		
		DatabaseObjectImpl dbo = null;
		if (visitor.optimize ()) {
			dbo = new DatabaseObjectImpl (this, entity);
		}
		
		for (Document document : documents) {
			if (visitor.optimize ()) {
				dbo.document = document;
			} else {
				dbo = new DatabaseObjectImpl (this, entity, document);
			}
			boolean cancel = visitor.onRecord (dbo);
			if (cancel) {
				return null;
			}
		}
		return null;
	}

	private Object _query (String entity, Query.Construct construct, final Query query) throws DatabaseException {
		
		if (query == null) {
			return null;
		}
		
		boolean queryHasEntity = true;
		
		if (Lang.isNullOrEmpty (query.entity ())) {
			queryHasEntity = false;
		} else {
			entity = query.entity ();
		}
		
		checkNotNull (entity);
		
		tracer.log (Tracer.Level.Debug, "Query Entity {0}", entity);
		
		String cacheKey = construct.name () + query.name ();
		
		BasicDBObject 		mQuery 		= null;
		Map<String, Object> bindings 	= query.bindings ();
		
		boolean cacheable = queryHasEntity && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ());
		
		if (cacheable) {
			mQuery 		= (BasicDBObject)QueriesCache.get (cacheKey);
			tracer.log (Tracer.Level.Debug, "Query meta loaded from cache {0}", mQuery);
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

		tracer.log (Tracer.Level.Debug, "       Query {0}", mQuery);
		tracer.log (Tracer.Level.Debug, "    Bindings {0}", bindings);
		
		if (Query.Construct.select.equals (construct)) {
			FindIterable<Document> cursor = db.getCollection (entity).find (mQuery);
			
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
			
			Select select = query.select ();
			if (select != null && select.count () > 0) {
				String [] pFields = new String [select.count ()];
				for (int i = 0; i < select.count (); i++) {
					System.out.println (select.get (i));
					pFields [i] = select.get (i);
				}
				cursor.projection (Projections.include (pFields));
			}

			return cursor;
		} else if (Query.Construct.delete.equals (construct)) {
			return db.getCollection (entity).deleteMany (mQuery);
		}
		
		return null;
		
	}
	
	private BasicDBObject applyBindings (BasicDBObject mQuery, Map<String, Object> bindings) {
		if (mQuery == null || bindings == null || bindings.isEmpty ()) {
			return mQuery;
		}
		
		mQuery = (BasicDBObject)mQuery.copy ();
		
		// TODO: apply bindings
		
		return mQuery;
	}
	
	// REF: https://docs.mongodb.com/manual/reference/method/db.collection.find/
	private CompiledQuery compile (String entity, Query.Construct construct, final Query query) {
		
		BasicDBObject mq = new BasicDBObject ();
		
		CompiledQuery cQuery = new CompiledQuery () {
			@Override
			public Object query () {
				return mq;
			}
			
			@Override
			public Map<String, Object> bindings () {
				return query.bindings ();
			}
		};
		
		// where
		Where where = query.where ();
		if (where == null || where.count () == 0) {
			return cQuery;
		}
		
		// Selectors
		Iterator<String> fields = where.conditions ();
		while (fields.hasNext ()) {
			String f = fields.next ();
			Object condOrFilter = where.get (f);
			if (Condition.class.isAssignableFrom (condOrFilter.getClass ())) {
				Condition c = (Condition)condOrFilter;
				BasicDBObject criteria = (BasicDBObject)mq.get (c.field ());
				
				boolean newlyCreated = false;
				if (criteria == null) {
					newlyCreated = true;
					criteria = new BasicDBObject ();
					mq.put (c.field (), criteria);
				}
				
				FilterAppender fa = FilterAppenders.get (c.operator ());
				if (fa == null) {
					fa = DefaultFilterAppender;
				}
				
				BasicDBObject filter = fa.append (c, criteria);
				if (filter != null) {
					mq.putAll ((Map<String, Object>)filter);
					if (newlyCreated) {
						mq.remove (c.field ());
					}
				}
			}
		}
		
		// Aggregates / ?

		return cQuery;
		
	}
	
	private void checkNotNull (String entity) throws DatabaseException {
		if (Lang.isNullOrEmpty (entity)) {
			throw new DatabaseException ("entity name is null");
		}
	}
	
	@Override
	public Object get () {
		return null;
	}

	@Override
	public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
		
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
	
}

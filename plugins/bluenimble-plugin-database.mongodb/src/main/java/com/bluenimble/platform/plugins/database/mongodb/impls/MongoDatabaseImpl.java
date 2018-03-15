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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
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
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.Query.Operator;
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
import com.mongodb.client.result.DeleteResult;

/**
 * 
 * TODO:
 * 	- near and within operators
 * 	- search on relations 
 * 	- remove, add to lists
 * 	- pop, popOne
 *	- start, page, sort, projections
 * 
 * 
 **/
public class MongoDatabaseImpl implements Database {

	private static final long serialVersionUID = 3547537996525908902L;
	
	private static final String 	DateFormat 			= "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	public static final String		CacheQueriesBucket	= "__plugin/database/odb/QueriesBucket__";

	private static final Set<String> SystemEntities = new HashSet<String> ();
	static {
		SystemEntities.add ("OSchedule");
	}
	
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
	
	public MongoDatabaseImpl (MongoDatabase db, Tracer tracer) {
		this.db 		= db;
		this.tracer 	= tracer;
	}

	@Override
	public DatabaseObject create (String name) throws DatabaseException {
		return new DatabaseObjectImpl (this, name);
	}

	@Override
	public void trx () {
		// 
	}
	
	@Override
	public void commit () throws DatabaseException {
		
	}

	@Override
	public void rollback () throws DatabaseException {
		
	}

	@Override
	public void createEntity (String eType, Field... fields) throws DatabaseException {
		eType = checkNotNull (eType);
		
		
	}

	@Override
	public void createIndex (String eType, IndexType type, String name,
			Field... fields) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		if (fields == null || fields.length == 0) {
			throw new DatabaseException ("entity " + eType + ". fields required to create an index");
		}
		
		
	}

	@Override
	public void dropIndex (String eType, String name) throws DatabaseException {

		eType = checkNotNull (eType);
		
		
	}

	@Override
	public int increment (DatabaseObject object, String field, int value) throws DatabaseException {
		return 0;
	}

	@Override
	public DatabaseObject get (String eType, Object id) throws DatabaseException {
		Document document = _get (eType, id);
	    if (document == null) {
		    return null;
	    }
		
		return new DatabaseObjectImpl (this, eType, document);

	}
	
	Document _get (String eType, Object id) {
		if (id == null) {
			return null;
		}
		
		MongoCollection<Document> collection = db.getCollection (eType);
		if (collection == null) {
			return null;
		}
		
		Object _id = id;
		if (ObjectId.isValid (String.valueOf (id))) {
			_id = new ObjectId (String.valueOf (id));
		}
		
	    return collection.find (eq (DatabaseObjectImpl.ObjectIdKey, _id)).first ();
	}
	
	@Override
	public List<DatabaseObject> find (String name, Query query, Visitor visitor) throws DatabaseException {
		
		FindIterable<Document> result;
		try {
			result = (FindIterable<Document>)_query (name, Query.Construct.select, query, false);
		} catch (Exception e) {
			throw new DatabaseException (e.getMessage (), e);
		}
		if (result == null) {
			return null;
		}
		
		return toList (name, result, visitor);
	}

	@Override
	public DatabaseObject findOne (String type, Query query) throws DatabaseException {
		
		// force count to 1
		query.count (1);
		
		List<DatabaseObject> result = find (type, query, null);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);
	}

	@Override
	public int delete (String eType, Object id) throws DatabaseException {
		
		checkNotNull (eType);
		
		if (id == null) {
			throw new DatabaseException ("can't delete object (missing object id)");
		}
		
		MongoCollection<Document> collection = db.getCollection (eType);
		if (collection == null) {
			return 0;
		}
		
		DeleteResult result = collection.deleteOne (eq (DatabaseObjectImpl.ObjectIdKey, new ObjectId (String.valueOf (id))));
		
		return (int)result.getDeletedCount ();
		
	}

	@Override
	public void drop (String eType) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		MongoCollection<Document> collection = db.getCollection (eType);
		if (collection == null) {
			return;
		}
		
		collection.drop ();
	}

	@Override
	public long count (String eType) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		MongoCollection<Document> collection = db.getCollection (eType);
		if (collection == null) {
			return 0;
		}
		
		return collection.count ();
	}

	@Override
	public int delete (Query query) throws DatabaseException {
		if (query == null) {
			return 0;
		}
		Object result = _query (null, Query.Construct.delete, query, false);
		if (result == null) {
			return 0;
		}
		return (Integer)result;
	}

	@Override
	public void recycle () {
	}
	
	@Override
	public void add (DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		//addRemove (CollectionAddQuery, parent, collection, child);
	}

	@Override
	public void remove (DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		//addRemove (CollectionRemoveQuery, parent, collection, child);
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
			if (SystemEntities.contains (collection)) {
				continue;
			}
			JsonObject oEntity = new JsonObject ();
			oEntity.set (Describe.Name, collection);
			oEntity.putAll (db.runCommand (new Document (SpiDescribe.CollStats, collection)));
			aEntities.add (oEntity);
			
			size += Json.getLong (oEntity, SpiDescribe.CollStats, 0);
			
		}
		
		describe.set (Describe.Size, size);
		
		return describe;
	}
	
	private void addRemove (String queryTpl, DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		
		if (parent == null || child == null) {
			return;
		}
		
		Document parentDoc = ((DatabaseObjectImpl)parent).document;
		Document childDoc 	= ((DatabaseObjectImpl)child).document;
		
		if (!((DatabaseObjectImpl)parent).persistent) {
			throw new DatabaseException ("Parent Object " + parent.entity () + " is not a persistent object");
		}
		
		if (!((DatabaseObjectImpl)child).persistent) {
			throw new DatabaseException ("Child Object " + child.entity () + " is not a persistent object");
		}
		
		// TODO
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
		
		return result;
		
	}

	@Override
	public DatabaseObject popOne (String name, Query query) throws DatabaseException {
		
		// force count to 1
		query.count (1);
		
		List<DatabaseObject> result = (List<DatabaseObject>)pop (name, query, null);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DatabaseObject> pop (String name, Query query, Visitor visitor) throws DatabaseException {
		if (query == null) {
			return null;
		}
		
		// TODO
		
		return null;
	}

	@Override
	public void schedule (String event, Query query, String cron) throws DatabaseException {
		if (query == null || Lang.isNullOrEmpty (query.entity ()) || query.construct () == null) {
			throw new DatabaseException ("query to schedule should provide the entity name and the construct [insert, update or delete]");
		}
		
		// TODO
		
	}

	@Override
	public void unschedule (String event) throws DatabaseException {
		
	}
	
	@Override
	public void exp (Set<String> entities, OutputStream out, Map<ExchangeOption, Boolean> options, final ExchangeListener listener) throws DatabaseException {
		
		// TODO
        
	}

	@Override
	public void imp (Set<String> entities, InputStream in, Map<ExchangeOption, Boolean> options, final ExchangeListener listener) throws DatabaseException {
		// TODO
	}
	
	private List<DatabaseObject> toList (String type, FindIterable<Document> documents, Visitor visitor) {
		
		if (visitor == null) {
			List<Document> list = new ArrayList<Document>();
			for (Document document : documents) {
				list.add (document);
			}
			return new DatabaseObjectList<DatabaseObject> (this, type, list);
		}
		
		DatabaseObjectImpl entity = null;
		if (visitor.optimize ()) {
			entity = new DatabaseObjectImpl (this, type);
		}
		
		for (Document document : documents) {
			if (visitor.optimize ()) {
				entity.document = document;
			} else {
				entity = new DatabaseObjectImpl (this, type, document);
			}
			boolean cancel = visitor.onRecord (entity);
			if (cancel) {
				return null;
			}
		}
		return null;
	}

	private Object _query (String type, Query.Construct construct, final Query query, boolean returnBefore) throws DatabaseException {
		
		if (query == null) {
			return null;
		}
		
		if (Query.Construct.select.equals (construct)) {
			returnBefore = false;
		}
		
		boolean queryHasEntity = true;
		
		String entity = query.entity ();
		
		if (Lang.isNullOrEmpty (entity)) {
			queryHasEntity = false;
			entity = type;
		}
		
		entity = checkNotNull (entity);
		
		tracer.log (Tracer.Level.Debug, "Query Entity {0}", entity);
		
		/*
		if (!"Collection Exists") {
			tracer.log (Tracer.Level.Debug, "Entity {0} not found", entity);
			return null;
		}
		*/
		
		String cacheKey = construct.name () + query.name ();
		
		BasicDBObject 		mQuery 		= null;
		Map<String, Object> bindings 	= query.bindings ();
		
		boolean cacheable = queryHasEntity && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ());
		
		if (cacheable) {
			mQuery 		= (BasicDBObject)QueriesCache.get (cacheKey);
			tracer.log (Tracer.Level.Debug, "Query meta loaded from cache {0}", mQuery);
		} 
		
		if (mQuery == null) {
			
			CompiledQuery cQuery = compile (entity, construct, query, returnBefore);
			
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
			FindIterable<Document> result = db.getCollection (entity).find (mQuery);
			// TODO: Test on Empty
			if (result == null/* || result.isEmpty ()*/) {
				return null;
			}
			
			return result;
		} else if (Query.Construct.delete.equals (construct)) {
			DeleteResult dr = db.getCollection (entity).deleteMany (mQuery);
			return dr.getDeletedCount ();
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
	private CompiledQuery compile (String entity, Query.Construct construct, final Query query, final boolean returnBefore) {
		
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
		
		// Projections
		
		
		// Aggregates / ?

		return cQuery;
		
	}
	
	/*
	private CompiledQuery compile (String entity, Query.Construct construct, final Query query, final boolean returnBefore) throws DatabaseException {
		final String fEntity = entity;
		QueryCompiler compiler = new SqlQueryCompiler (construct) {
			private static final long serialVersionUID = -1248971549807669897L;
			
			@Override
			protected void onQuery (Timing timing, Query query)
					throws DatabaseException {
				super.onQuery (timing, query);
				
				if (Timing.start.equals (timing)) {
					return;
				}
				
				if (query.start () > 0) {
					buff.append (Lang.SPACE).append (Sql.Skip).append (Lang.SPACE).append (query.start ());
				}
				if (query.count () > 0) {
					buff.append (Lang.SPACE).append (Sql.Limit).append (Lang.SPACE).append (query.count ());
				}
			}
			
			@Override
			protected void onSelect (Timing timing, Select select) throws DatabaseException {
				super.onSelect (timing, select);
				if (Timing.end.equals (timing) && returnBefore) {
					buff.append (Lang.SPACE).append (Sql.ReturnBefore);
				}
			}
			
			@Override
			protected String operatorFor (Operator operator) {
				if (Operator.ftq.equals (operator)) {
					return Lucene;
				}
				return super.operatorFor (operator);
			}
			
			@Override
			protected void entity () {
				buff.append (fEntity);
			}
		}; 
		
		return compiler.compile (query);
		
	}
	*/
	
	private String checkNotNull (String eType) throws DatabaseException {
		if (Lang.isNullOrEmpty (eType)) {
			throw new DatabaseException ("entity name is null");
		}
		return eType;
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
	
}

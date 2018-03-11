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
package com.bluenimble.platform.plugins.database.orientdb.impls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.Caching.Target;
import com.bluenimble.platform.db.query.CompiledQuery;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.Query.Operator;
import com.bluenimble.platform.db.query.QueryCompiler;
import com.bluenimble.platform.db.query.Select;
import com.bluenimble.platform.db.query.impls.SqlQueryCompiler;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.function.OFunction;
import com.orientechnologies.orient.core.metadata.function.OFunctionLibrary;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.schedule.OScheduledEvent;
import com.orientechnologies.orient.core.schedule.OScheduledEventBuilder;
import com.orientechnologies.orient.core.schedule.OScheduler;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class OrientDatabase implements Database {

	private static final long serialVersionUID = 3547537996525908902L;
	
	private static final String 	DateFormat 			= "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	public static final String		CacheQueriesBucket	= "__plugin/database/odb/QueriesBucket__";
	private static final String 	Lucene 				= "LUCENE";
	
	private static final String 	FunctionPostfix		= "_fct";
	
	private interface Tokens {
		String Type 		= "{Type}";
		String Field 		= "{field}";
		String Parent 		= "{parent}";
		String Child 		= "{child}";
		String Collection 	= "{collection}";
		String Value 		= "{value}";
	}
	
	private interface Sql {
		String Skip			= "skip";
		String Limit		= "limit";
		String ReturnBefore	= "return before";
	}
	
	private static final Map<IndexType, INDEX_TYPE> IndexTypes = new HashMap<IndexType, INDEX_TYPE> ();
	static {
		IndexTypes.put (IndexType.Unique, INDEX_TYPE.UNIQUE);
		IndexTypes.put (IndexType.NotUnique, INDEX_TYPE.NOTUNIQUE);
	}
	
	private static final Map<Field.Type, OType> FieldTypes = new HashMap<Field.Type, OType> ();
	static {
		FieldTypes.put (Field.Type.Boolean, OType.BOOLEAN);
		FieldTypes.put (Field.Type.Byte, 	OType.BYTE);
		FieldTypes.put (Field.Type.Binary, 	OType.BINARY);
		FieldTypes.put (Field.Type.Short, 	OType.SHORT);
		FieldTypes.put (Field.Type.Long, 	OType.LONG);
		FieldTypes.put (Field.Type.Integer, OType.INTEGER);
		FieldTypes.put (Field.Type.Double, 	OType.DOUBLE);
		FieldTypes.put (Field.Type.Float, 	OType.FLOAT);
		FieldTypes.put (Field.Type.Decimal, OType.DECIMAL);
		FieldTypes.put (Field.Type.Date,	OType.DATE);
		FieldTypes.put (Field.Type.DateTime,OType.DATETIME);
		FieldTypes.put (Field.Type.String,	OType.STRING);
	}
	
    private static final Set<String> SystemEntities = new HashSet<String> ();
	static {
		SystemEntities.add ("OSchedule");
		SystemEntities.add ("OFunction");
		SystemEntities.add ("ORestricted");
		SystemEntities.add ("OUser");
		SystemEntities.add ("OIdentity");
		SystemEntities.add ("OTriggered");
		SystemEntities.add ("OSequence");
		SystemEntities.add ("ORole");
		SystemEntities.add ("V");
		SystemEntities.add ("E");
		SystemEntities.add ("_studio");
	}
	
	private static final String DeleteQuery 			= "DELETE FROM " + Tokens.Type + " WHERE " + Fields.Id + " = :" + Fields.Id;
	private static final String GetQuery 				= "SELECT FROM " + Tokens.Type + " WHERE " + Fields.Id + " = :" + Fields.Id;

	private static final String CollectionAddQuery 		= "UPDATE " + Tokens.Parent + " ADD " + Tokens.Collection + " = " + Tokens.Child;
	private static final String CollectionRemoveQuery 	= "UPDATE " + Tokens.Parent + " REMOVE " + Tokens.Collection + " = " + Tokens.Child;

	private static final String IncrementQuery 			= "UPDATE " + Tokens.Type + " INCREMENT " + Tokens.Field + " = " + Tokens.Value + " RETURN AFTER WHERE " + Fields.Id + " = :" + Fields.Id + 
															" LOCK RECORD";
	
	interface Describe {
		String Size 		= "size";
		String Entities 	= "entities";
			String Name 	= "name";
			String Count 	= "count";
	}
	
	private Map<String, String> QueriesCache = new ConcurrentHashMap<String, String> ();
	
	private ODatabaseDocumentTx db;
	private Tracer				tracer;
	
	public OrientDatabase (ODatabaseDocumentTx db, Tracer tracer) {
		this.db 		= db;
		this.tracer 	= tracer;
		this.db.getStorage ().getConfiguration ().dateTimeFormat = DateFormat;
	}

	@Override
	public DatabaseObject create (String name) throws DatabaseException {
		return new DatabaseObjectImpl (this, name);
	}

	@Override
	public void trx () {
		db.begin ();
	}
	
	@Override
	public void commit () throws DatabaseException {
		db.commit ();
	}

	@Override
	public void rollback () throws DatabaseException {
		db.rollback ();
	}

	@Override
	public void createEntity (String eType, Field... fields) throws DatabaseException {
		eType = checkNotNull (eType);
		
		if (fields == null || fields.length == 0) {
			throw new DatabaseException ("entity " + eType + ". fields missing");
		}
		
		OClass oClass = db.getMetadata ().getSchema ().getClass (eType);
		if (oClass != null) {
			throw new DatabaseException ("entity " + eType + " already exists");
		}
		
		oClass = db.getMetadata ().getSchema ().createClass (eType);
		
		String [] props = new String [fields.length];
		
		for (int i = 0; i < fields.length; i++) {
			Field f = fields [i];
			props [i] = f.name ();
			if (f.type () != null) {
				OProperty property = oClass.createProperty (f.name (), FieldTypes.get (f.type ()));
				property.setNotNull (f.required ());
				if (f.unique ()) {
					property.createIndex (INDEX_TYPE.UNIQUE);
				}
			}
		}
		
		db.getMetadata ().getSchema ().save ();
	}

	@Override
	public void createIndex (String eType, IndexType type, String name,
			Field... fields) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		if (fields == null || fields.length == 0) {
			throw new DatabaseException ("entity " + eType + ". fields required to create an index");
		}
		
		boolean save = false;
		
		OClass oClass = db.getMetadata ().getSchema ().getClass (eType);
		if (oClass == null) {
			oClass = db.getMetadata ().getSchema ().createClass (eType);
			save = true;
		}
		
		String [] props = new String [fields.length];
		
		for (int i = 0; i < fields.length; i++) {
			Field f = fields [i];
			props [i] = f.name ();
			if (f.type () != null) {
				oClass.createProperty (f.name (), FieldTypes.get (f.type ()));
			}
		}
		
		switch (type) {
			case Text:
				oClass.createIndex (eType + Lang.DOT + name, "FULLTEXT", null, null, Lucene, props);
				break;
			case Spacial:
				oClass.createIndex (eType + Lang.DOT + name, "SPATIAL", null, null, Lucene, props);
				break;
			default:
				oClass.createIndex (eType + Lang.DOT + name, IndexTypes.get (type), props);
				break;
		}
		if (save) {
			db.getMetadata ().getSchema ().save ();
		}
	}

	@Override
	public void dropIndex (String eType, String name) throws DatabaseException {

		eType = checkNotNull (eType);
		
		if (!db.getMetadata ().getSchema ().existsClass (eType)) {
			return;
		}
		
		db.getMetadata ().getIndexManager (). dropIndex (eType + Lang.DOT + name);
	}

	@Override
	public int increment (DatabaseObject object, String field, int value) throws DatabaseException {
		
		ODocument doc = ((DatabaseObjectImpl)object).document;
		
		String query = format (Lang.replace (IncrementQuery, Tokens.Value, String.valueOf (value)), doc.getClassName (), field);
		
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put (Fields.Id, doc.field (Fields.Id));
		
		OResultSet<ODocument> result = db.command (new OCommandSQL (query)).execute (params);
		
		ODocument document = (ODocument)result.get (0);
		
		return document.field (field);
		
	}

	@Override
	public DatabaseObject get (String eType, Object id) throws DatabaseException {
		
		if (id == null) {
			return null;
		}
		
		ODocument doc = _get (eType, id);
		if (doc == null) {
			return null;
		}
		
		return new DatabaseObjectImpl (this, doc);

	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<DatabaseObject> find (String name, Query query, Visitor visitor) throws DatabaseException {
		List<ODocument> result;
		try {
			result = (List<ODocument>)_query (name, Query.Construct.select, query, false);
		} catch (Exception e) {
			throw new DatabaseException (e.getMessage (), e);
		}
		if (result == null || result.isEmpty ()) {
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
		
		if (!db.getMetadata ().getSchema ().existsClass (eType)) {
			return 0;
		}
		
		OCommandSQL command = new OCommandSQL (format (DeleteQuery, eType));
		
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put (Fields.Id, id);
		
		return db.command (command).execute (params);
		
	}

	@Override
	public void drop (String eType) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		if (!db.getMetadata ().getSchema ().existsClass (eType)) {
			return;
		}
		
		db.getMetadata ().getSchema ().dropClass (eType);
		
		db.getMetadata ().getSchema ().reload ();
	}

	@Override
	public long count (String eType) throws DatabaseException {
		
		eType = checkNotNull (eType);
		
		if (!db.getMetadata ().getSchema ().existsClass (eType)) {
			return 0;
		}
		
		return db.getMetadata ().getSchema ().getClass (eType).count ();
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
		if (db != null) {
			tracer.log (Tracer.Level.Info, "Recycling database connection {0}", db);
			db.activateOnCurrentThread ();
			db.close ();
		}
	}
	
	@Override
	public void add (DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		addRemove (CollectionAddQuery, parent, collection, child);
	}

	@Override
	public void remove (DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		addRemove (CollectionRemoveQuery, parent, collection, child);
	}

	@Override
	public JsonObject describe () {
		
		JsonObject describe = new JsonObject ();
		
		describe.set (Describe.Size, db.getSize ());
		
		Collection<OClass> entities = db.getMetadata ().getSchema ().getClasses ();
		if (entities == null || entities.isEmpty ()) {
			return describe;
		}
		
		JsonArray aEntities = new JsonArray ();
		describe.set (Describe.Entities, aEntities);
		
		for (OClass entity : entities) {
			if (SystemEntities.contains (entity.getName ())) {
				continue;
			}
			JsonObject oEntity = new JsonObject ();
			oEntity.set (Describe.Name, entity.getName ());
			oEntity.set (Describe.Count, entity.count ());
			aEntities.add (oEntity);
		}
		
		return describe;
	}
	
	private void addRemove (String queryTpl, DatabaseObject parent, String collection, DatabaseObject child)
			throws DatabaseException {
		
		if (parent == null || child == null) {
			return;
		}
		
		ODocument parentDoc = ((DatabaseObjectImpl)parent).document;
		ODocument childDoc 	= ((DatabaseObjectImpl)child).document;
		
		if (parentDoc.getIdentity () == null || !parentDoc.getIdentity ().isPersistent ()) {
			throw new DatabaseException ("Parent Object " + parent.entity () + " is not a persistent object");
		}
		
		if (childDoc.getIdentity () == null || !childDoc.getIdentity ().isPersistent ()) {
			throw new DatabaseException ("Child Object " + child.entity () + " is not a persistent object");
		}
		
		String query = format (
			queryTpl, 
			parentDoc.getIdentity ().toString (), 
			collection, 
			childDoc.getIdentity ().toString ()
		);
		
		db.command (new OCommandSQL (query)).execute ();
	}

	private ODocument _get (String type, Object id) {
		
		if (Lang.isNullOrEmpty (type)) {
			return null;
		}
		
		if (!db.getMetadata ().getSchema ().existsClass (type)) {
			return null;
		}
		
		String query = format (GetQuery, type);
		
		OSQLSynchQuery<ODocument> q = 
				new OSQLSynchQuery<ODocument> (query, 1);
		
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put (Fields.Id, id);
		
		List<ODocument> result = db.command (q).execute (params);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);

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
		
		db.declareIntent (new OIntentMassiveInsert ());

		try {
			Iterator<String> entities = data.keys ();
			while (entities.hasNext ()) {
				int count = 0;
				String entityName = entities.next ();
				Object oRecords = data.get (entityName);
				if (!(oRecords instanceof JsonArray)) {
					continue;
				}
				
				JsonArray records = Json.getArray (data, entityName);
				for (int i = 0; i < records.count (); i++) {
					Object oRec = records.get (i);
					if (!(oRec instanceof JsonObject)) {
						continue;
					}
					
					// TODO: Reuse object
					DatabaseObject entity = create (entityName);
					// TODO: REVIEW
					
					// set data
					entity.load ((JsonObject)oRec);
					// put record
					entity.save ();
					// clear object
					entity.clear ();
					
					count++;
				}
				result.set (entityName, count);
				result.set (Database.Fields.Total, Json.getInteger (result, Database.Fields.Total, 0) + count);
			}
		} finally {
			db.declareIntent (null);
		}
		
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
		List<ODocument> result = (List<ODocument>)_query (null, Query.Construct.delete, query, true);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return toList (null, result, visitor);
	}

	@Override
	public void schedule (String event, Query query, String cron) throws DatabaseException {
		if (query == null || Lang.isNullOrEmpty (query.entity ()) || query.construct () == null) {
			throw new DatabaseException ("query to schedule should provide the entity name and the construct [insert, update or delete]");
		}
		
		try {
			OScheduler sch = db.getMetadata ().getScheduler ();
			
			OScheduledEvent oEvent = sch.getEvent (event);
			if (oEvent != null) {
				sch.removeEvent (event);
			}
			
			sch.scheduleEvent (
				new OScheduledEventBuilder ().setName (event).setRule (cron).setFunction (
					newFunction (event, query)
				).build ()
			);
			
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
	}

	@Override
	public void unschedule (String event) throws DatabaseException {
		try {
			db.getMetadata ().getScheduler ().removeEvent (event);
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
	}
	
	@Override
	public void exp (Set<String> entities, OutputStream out, Map<ExchangeOption, Boolean> options, final ExchangeListener listener) throws DatabaseException {
		
		OCommandOutputListener olistener = new OCommandOutputListener () {
            @Override
            public void onMessage (String iText) {
            	if (listener != null) {
            		listener.onMessage (iText);
            	}
            }
        };

        ODatabaseExport export = null;
		try {
			export = new ODatabaseExport (db, out, olistener);
		} catch (IOException e) {
			throw new DatabaseException (e.getMessage (), e);
		}

        export.setIncludeInfo (option (options, ExchangeOption.info, false));
        export.setIncludeClusterDefinitions (option (options, ExchangeOption.entities, false));
        export.setIncludeSchema (option (options, ExchangeOption.schema, false));
        export.setIncludeIndexDefinitions (option (options, ExchangeOption.indexes, false));
        export.setIncludeManualIndexes (option (options, ExchangeOption.indexes, false));
        export.setPreserveRids (option (options, ExchangeOption.ids, false));
        
        export.setOptions ("-compressionLevel=9");

        if (entities != null && !entities.isEmpty ()) {
            export.setIncludeClasses (entities);
        }
        
        export.exportDatabase ();
        export.close ();
        
	}

	@Override
	public void imp (Set<String> entities, InputStream in, Map<ExchangeOption, Boolean> options, final ExchangeListener listener) throws DatabaseException {
		OCommandOutputListener olistener = new OCommandOutputListener () {
            @Override
            public void onMessage (String iText) {
            	if (listener != null) {
            		listener.onMessage (iText);
            	}
            }
        };

        ODatabaseImport _import = null;
		try {
			_import = new ODatabaseImport (db, in, olistener);
		} catch (IOException e) {
			throw new DatabaseException (e.getMessage (), e);
		}

		_import.setIncludeInfo (option (options, ExchangeOption.info, false));
		_import.setIncludeClusterDefinitions (option (options, ExchangeOption.entities, false));
		_import.setIncludeSchema (option (options, ExchangeOption.schema, false));
		_import.setIncludeIndexDefinitions (option (options, ExchangeOption.indexes, false));
		_import.setIncludeManualIndexes (option (options, ExchangeOption.indexes, false));
		_import.setPreserveRids (option (options, ExchangeOption.ids, false));
		
		_import.setDeleteRIDMapping (false);

        if (entities != null && !entities.isEmpty ()) {
        	_import.setIncludeClasses (entities);
        }
        
        _import.importDatabase ();
        _import.close ();
	}
	
	private List<DatabaseObject> toList (String type, List<ODocument> documents, Visitor visitor) {
		if (visitor == null) {
			return new DatabaseObjectList<DatabaseObject> (this, documents);
		}
		
		DatabaseObjectImpl entity = null;
		if (visitor.optimize ()) {
			entity = new DatabaseObjectImpl (this, type);
		}
		
		for (ODocument document : documents) {
			if (visitor.optimize ()) {
				entity.document = document;
			} else {
				entity = new DatabaseObjectImpl (this, document);
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
		
		if (!db.getMetadata ().getSchema ().existsClass (entity)) {
			tracer.log (Tracer.Level.Debug, "Entity {0} not found", entity);
			return null;
		}
		
		String cacheKey = construct.name () + query.name ();
		
		String 				sQuery 		= null;
		Map<String, Object> bindings 	= query.bindings ();
		
		if (queryHasEntity && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ())) {
			sQuery 		= (String)QueriesCache.get (cacheKey);
			tracer.log (Tracer.Level.Debug, "Query meta loaded from cache {0}", sQuery);
		} 
		
		if (sQuery == null) {
			
			CompiledQuery cQuery = compile (entity, construct, query, returnBefore);
			
			sQuery 		= cQuery.query 		();
			bindings	= cQuery.bindings 	();
			
			if (queryHasEntity && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ())) {
				QueriesCache.put (cacheKey, sQuery);
				tracer.log (Tracer.Level.Debug, "Query meta stored in cache {0}", sQuery);
			} 
		}
		
		tracer.log (Tracer.Level.Debug, "\tQuery {0}", sQuery);
		tracer.log (Tracer.Level.Debug, "\tBindings: {0}", bindings);
		
		if (Query.Construct.select.equals (construct)) {
			OSQLSynchQuery<ODocument> q = new OSQLSynchQuery<ODocument> (sQuery);
			List<ODocument> result = db.command (q).execute (bindings);
			if (result == null || result.isEmpty ()) {
				return null;
			}
			return result;
		} else {
			return db.command (new OCommandSQL (sQuery)).execute (bindings);
		}
	}
	
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

	private String format (String query, String type) {
		return Lang.replace (query, Tokens.Type, type);
	}
	
	private String format (String query, String type, String field) {
		return Lang.replace (format (query, type), Tokens.Field, field);
	}
	
	private String format (String query, String parent, String collection, String child) {
		return Lang.replace (Lang.replace (Lang.replace (query, Tokens.Collection, collection), Tokens.Parent, parent), Tokens.Child, child);
	}
	
	private String checkNotNull (String eType) throws DatabaseException {
		if (Lang.isNullOrEmpty (eType)) {
			throw new DatabaseException ("entity name is null");
		}
		return eType;
	}
	
	private OFunction newFunction (String event, Query query) throws DatabaseException {
		
		OFunctionLibrary fLibrary = db.getMetadata ().getFunctionLibrary ();
		
		String fName = event + FunctionPostfix;
		
		OFunction function = fLibrary.getFunction (fName);
		if (function != null) {
			fLibrary.dropFunction (function);
		}
		
		function = fLibrary.createFunction (event + FunctionPostfix);
		function.setLanguage ("SQL");
		function.setCode (compile (query.entity (), query.construct (), query, false).query ());
		function.save ();
		
		return function;
	}
	
	private boolean option (Map<ExchangeOption, Boolean> options, ExchangeOption option, boolean defaultValue) {
		if (options == null) {
			return defaultValue;
		}
		if (!options.containsKey (option)) {
			return defaultValue;
		}
		return options.get (option);
	}

	@Override
	public Object get () {
		return null;
	}

	@Override
	public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
		
	}

}

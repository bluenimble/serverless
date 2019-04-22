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
package com.bluenimble.platform.indexer.impls;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.ValueHolder;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.utils.ContentTypes;
import com.bluenimble.platform.indexer.Indexer;
import com.bluenimble.platform.indexer.IndexerException;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.Caching.Target;
import com.bluenimble.platform.query.CompiledQuery;
import com.bluenimble.platform.query.Query;
import com.bluenimble.platform.query.Query.Operator;
import com.bluenimble.platform.query.QueryCompiler;
import com.bluenimble.platform.query.QueryException;
import com.bluenimble.platform.query.Select;
import com.bluenimble.platform.query.impls.SqlQueryCompiler;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.Serializer;

public class ElasticSearchIndexer implements Indexer {
	
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	private interface Internal {
		String 	Id 			= "id";
		String 	Timestamp 	= "timestamp";
		
		interface Elk {
			String Id 		= "_id";
			String Index 	= "_index";
			String Type 	= "_type";
			String Create	= "_create";
			String Update 	= "_update";
			//String UpdateByQuery
			//				= "_update_by_query";
			String Bulk 	= "_bulk";
			String Search 	= "_search";
			
			String Count 	= "_count";
			String CountField
							= "count";
			String Hits 	= "hits";
			String Source 	= "_source";
			String Mapping 	= "_mapping";
			String Query 	= "query";
			String MatchAll = "match_all";
			String DeleteQ 	= "_delete_by_query?conflicts=proceed";
			String Size		= "size";
			
			interface Operations {
				String Index = "index";
			}
			interface Sql {
				String Query	= "query";
				String Path		= "/_sql";
				String Offset 	= "OFFSET";
				String Limit 	= "LIMIT";
			}
			interface Result {
				String Columns 	= "columns";
				String Rows 	= "rows";
			} 
		}
	}
	
	private 			Remote remote;
	private 			String index;
	
	private 			Tracer tracer;
	
	private Map<String, String> QueriesCache = new ConcurrentHashMap<String, String> ();

	public ElasticSearchIndexer (Remote remote, String index, Tracer tracer) {
		this.remote 	= remote;
		this.index 		= index;
		this.tracer 	= tracer;
	}
	
	@Override
	public boolean exists (String entity) throws IndexerException {
		
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		if (Lang.isNullOrEmpty (entity)) {
			throw new IndexerException ("Entity cannot be null nor empty.");
		}
		
		tracer.log (Tracer.Level.Info, "Checking if entity [{0}] exists.", entity);
		
		ElkError error = new ElkError ();
		
		remote.head (
			(JsonObject)new JsonObject ().set (Remote.Spec.Path, Internal.Elk.Mapping + Lang.SLASH + entity),
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onDone (int code, Object message) {
				}
			}
		);
		
		if (!error.happened ()) {
			return true;
		}
		
		if (error.code == 404) {
			return false;
		}
		
		throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
	}
	
	@Override
	public JsonObject create (String entity, JsonObject definition) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		if (Lang.isNullOrEmpty (entity)) {
			throw new IndexerException ("Entity cannot be null nor empty.");
		}
		
		tracer.log (Tracer.Level.Info, "Creating entity [{0}] with definition {1}", entity, definition);
		
		JsonObject result = new JsonObject ();
		ElkError error = new ElkError ();
		
		JsonObject oEntity = (JsonObject)new JsonObject ()
			.set (Remote.Spec.Path, entity + Lang.SLASH + Internal.Elk.Mapping)
			.set (Remote.Spec.Headers, 
				new JsonObject ()
					.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
			).set (Remote.Spec.Serializer, Serializer.Name.json);
		
		if (!Json.isNullOrEmpty (definition)) {
			oEntity.set (Remote.Spec.Data, definition);
		}
		
		remote.put (
			oEntity, 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject clear (String entity) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		tracer.log (Tracer.Level.Info, "Deleting all documents in entity [{0}]", entity);
		
		JsonObject result = new JsonObject ();
		ElkError error = new ElkError ();
		
		remote.post (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Path, entity (entity) + Internal.Elk.DeleteQ)
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
				).set (Remote.Spec.Data, 
					new JsonObject ()
						.set (Internal.Elk.Query, new JsonObject ()
							.set (Internal.Elk.MatchAll, new JsonObject ())
						)
				).set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		return null;
	}
	
	@Override
	public JsonObject describe (String entity) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		if (Lang.isNullOrEmpty (entity)) {
			throw new IndexerException ("Entity cannot be null nor empty.");
		}
		
		tracer.log (Tracer.Level.Info, "Describe entity [{0}]", entity);
		
		JsonObject result = new JsonObject ();
		ElkError error = new ElkError ();
		
		remote.get (
			(JsonObject)new JsonObject ()
			.set (Remote.Spec.Path, Internal.Elk.Mapping + Lang.SLASH + entity)
			.set (Remote.Spec.Headers, 
				new JsonObject ()
					.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
			).set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject put (String entity, JsonObject doc) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		if (Json.isNullOrEmpty (doc)) {
			throw new IndexerException ("Document cannot be null nor empty.");
		}
		
		String id = Json.getString (doc, Internal.Id);
		if (Lang.isNullOrEmpty (id)) {
			doc.set (Internal.Id, Lang.rand ());
		}
		
		if (!doc.containsKey (Internal.Timestamp)) {
			doc.set (Internal.Timestamp, utc ());
		}
		
		tracer.log (Tracer.Level.Info, "Indexing document [{0}]", id);
		
		JsonObject result = new JsonObject ();
		ElkError error = new ElkError ();
		
		remote.put (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Path, entity (entity) + id + Lang.SLASH + Internal.Elk.Create)
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
				).set (Remote.Spec.Data, doc)
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject get (String entity, String id) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		if (Lang.isNullOrEmpty (id)) {
			throw new IndexerException ("Document Id cannot be null.");
		}
		
		tracer.log (Tracer.Level.Info, "Lookup document [{0}]", id);
		
		JsonObject result = new JsonObject ();
		ElkError error = new ElkError ();
		
		remote.get (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Path, entity (entity) + id)
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject update (String entity, JsonObject doc, boolean partial) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		if (Json.isNullOrEmpty (doc)) {
			throw new IndexerException ("Document cannot be null nor empty.");
		}
		
		String id = Json.getString (doc, Internal.Id); 
		
		if (Lang.isNullOrEmpty (id)) {
			throw new IndexerException ("Document Id cannot be null.");
		}
		
		if (!partial) {
			tracer.log (Tracer.Level.Info, "It's not partial update. Create document [{0}]", id);
			return create (entity, doc);
		}
		
		tracer.log (Tracer.Level.Info, "Update partially document [{0}]", id);
		
		doc.remove (Internal.Id);
		
		JsonObject result = new JsonObject ();
		ElkError error = new ElkError ();
		
		remote.post (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Path, entity (entity) + id + Lang.SLASH + Internal.Elk.Update)
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
				).set (Remote.Spec.Data, doc)
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	@Override
	public JsonObject delete (String entity, String id) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		if (Lang.isNullOrEmpty (id)) {
			throw new IndexerException ("Document Id cannot be null nor empty.");
		}
		
		JsonObject result = new JsonObject ();
		ElkError error = new ElkError ();
		
		tracer.log (Tracer.Level.Info, "Delete document [{0}]", id);
		
		remote.delete (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Path, entity (entity) + id)
				.set (Remote.Spec.Serializer, Serializer.Name.json), 
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	private void sqlQuery (Query.Construct construct, Query query, Visitor visitor) throws IndexerException {
		if (query == null) {
			return;
		}
		
		tracer.log (Tracer.Level.Debug, "SQL Query {0}", query);
		
		String cacheKey = query.construct ().name () + query.name ();
		
		String 				sQuery 		= null;
		Map<String, Object> bindings 	= query.bindings ();
		
		if (query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ())) {
			sQuery 		= (String)QueriesCache.get (cacheKey);
			tracer.log (Tracer.Level.Debug, "Query meta loaded from cache {0}", sQuery);
		} 
		
		if (sQuery == null) {
			
			CompiledQuery cQuery = compile (construct, query);
			
			sQuery 		= (String)cQuery.query ();
			bindings	= cQuery.bindings ();
			
			if (query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ())) {
				QueriesCache.put (cacheKey, sQuery);
				tracer.log (Tracer.Level.Debug, "Query meta stored in cache {0}", sQuery);
			} 
		}
		
		tracer.log (Tracer.Level.Debug, "\tQuery {0}", sQuery);
		tracer.log (Tracer.Level.Debug, "\tBindings: {0}", bindings);
		
		if (Query.Construct.select.equals (construct)) {
			JsonObject oQuery = new JsonObject ();
			oQuery.set (Internal.Elk.Sql.Query, sQuery);
			
			ElkError error = new ElkError ();
			
			remote.post (
				(JsonObject)new JsonObject ()
					.set (Remote.Spec.Path, Internal.Elk.Sql.Path)
					.set (Remote.Spec.Headers, 
						new JsonObject ()
							.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
							.set (HttpHeaders.ACCEPT, ContentTypes.Json)
					).set (Remote.Spec.Data, oQuery)
					.set (Remote.Spec.Serializer, Serializer.Name.json),
				new Remote.Callback () {
					@Override
					public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
					}
					@Override
					public void onData (int code, byte [] data) {
					}
					@Override
					public void onError (int code, Object message) {
						error.set (code, message);
					}
					@Override
					public void onDone (int code, Object data) {
						if (data == null) {
							return;
						}
						JsonObject oData = (JsonObject)data;
						if (Json.isNullOrEmpty (oData)) {
							return;
						}
						
						JsonArray columns = Json.getArray (oData, Internal.Elk.Result.Columns);
						
						JsonArray rows = Json.getArray (oData, Internal.Elk.Result.Rows);
						if (Json.isNullOrEmpty (rows)) {
							return;
						}
						
						for (int i = 0; i < rows.count (); i++) {
							boolean cancel = visitor.onRecord (columns, (JsonObject)rows.get (i));
							if (cancel) {
								return;
							}
						}
					}
				}
			);
			
			if (error.happened ()) {
				throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
			}
			
		} else {
			// delete
		}		
	}
	
	@Override
	public void find (Query query, Visitor visitor) throws IndexerException {
		sqlQuery (Query.Construct.select, query, visitor);
	}
	
	@Override
	public JsonObject findOne (Query query) throws IndexerException {
		// force count to 1
		query.count (1);
		
		ValueHolder<JsonObject> result = new ValueHolder<JsonObject> ();
		
		sqlQuery (Query.Construct.select, query, new Visitor () {
			@Override
			public boolean onRecord (JsonArray columns, JsonObject record) {
				result.set (record);
				return false;
			}
			
		});
		return result.get ();
	}
	
	
	private CompiledQuery compile (Query.Construct construct, final Query query) throws IndexerException {
		QueryCompiler compiler = new SqlQueryCompiler (construct) {
			private static final long serialVersionUID = -1248971549807669897L;
			
			@Override
			protected void onQuery (Timing timing, Query query)
					throws QueryException {
				super.onQuery (timing, query);
				
				if (Timing.start.equals (timing)) {
					return;
				}
				
				if (query.start () > 0) {
					buff.append (Lang.SPACE).append (Internal.Elk.Sql.Offset).append (Lang.SPACE).append (query.start ());
				}
				if (query.count () > 0) {
					buff.append (Lang.SPACE).append (Internal.Elk.Sql.Limit).append (Lang.SPACE).append (query.count ());
				}
			}
			
			@Override
			protected void onSelect (Timing timing, Select select) throws QueryException {
				super.onSelect (timing, select);
			}
			
			@Override
			protected String operatorFor (Operator operator) {
				return super.operatorFor (operator);
			}
			
			@Override
			protected void entity () {
				buff.append (index);
			}
		}; 
		try {
			return compiler.compile (query);
		} catch (QueryException e) {
			throw new IndexerException (e.getMessage (), e);
		}
		
	}

	@Override
	public JsonObject search (JsonObject query, String [] entities) throws IndexerException {
		return _search (query, entities, false);
	}
	
	@Override
	public long count (JsonObject query, String [] entities) throws IndexerException {
		return Json.getLong (_search (query, entities, true), Internal.Elk.CountField, 0);
	}

	public JsonObject _search (JsonObject query, String [] entities, boolean isCount) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		String types = Lang.BLANK;
		if (entities != null && entities.length > 0) {
			types = Lang.join (entities, Lang.COMMA);
		}
		
		long size = Json.getLong (query, Internal.Elk.Size, -1);
		if (size == -1) {
			query.remove (Internal.Elk.Size);
		}
		
		tracer.log (
			Tracer.Level.Info, 
			"search documents in index {0} / [{1}] with query {2}", 
			index, 
			types.equals (Lang.BLANK) ? "All Entities" : Lang.join (entities), 
			query
		);
		
		ValueHolder<JsonObject> result = new ValueHolder<JsonObject> ();
		ElkError error = new ElkError ();
		
		remote.post (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Path, index + Lang.SLASH + types + 
						(types.equals (Lang.BLANK) ? Lang.BLANK : Lang.SLASH) + 
						(isCount ? Internal.Elk.Count : Internal.Elk.Search))
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.Json)
				).set (Remote.Spec.Data, query)
				.set (Remote.Spec.Serializer, Serializer.Name.json),
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data == null) {
						return;
					}
					JsonObject oData = (JsonObject)data;
					if (!isCount) {
						oData = result (Json.getObject (oData, Internal.Elk.Hits));
					}
					result.set (oData);
				}
			}
		);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result.get ();
	}
	
	private JsonObject result (JsonObject result) {
		
		JsonObject newResult = (JsonObject)new JsonObject ().set (ApiOutput.Defaults.Items, new JsonArray ());
		
		if (Json.isNullOrEmpty (result)) {
			return newResult;
		}
		
		JsonArray hits = Json.getArray (result, Internal.Elk.Hits);
		
		if (Json.isNullOrEmpty (hits)) {
			return newResult;
		}
		
		JsonArray array = Json.getArray (newResult, ApiOutput.Defaults.Items);
		
		for (int i = 0; i < hits.count (); i++) {
			array.add (Json.getObject ((JsonObject)hits.get (i), Internal.Elk.Source));
		}
		
		result.remove (Internal.Elk.Hits);
		
		return newResult.merge (result);
		
	}
	
	private String entity (String entity) {
		if (Lang.isNullOrEmpty (entity)) {
			return index + Lang.SLASH;
		}
		return index + Lang.SLASH + entity + Lang.SLASH;
	}
	
	class ElkError {
		int code;
		Object message;
		void set (int code, Object message) {
			this.code = code;
			this.message = message;
		}
		boolean happened () {
			return code > 0;
		}
	}

	@Override
	public JsonObject bulk (JsonObject payload) throws IndexerException {
		if (remote == null) {
			throw new IndexerException ("No Remoting feature attached to this indexer");
		}
		
		if (Json.isNullOrEmpty (payload)) {
			throw new IndexerException ("Document cannot be null nor empty.");
		}
		
		StringBuilder sPayload = new StringBuilder ();
		
		// prepare payload
		JsonObject 	create 		= Json.getObject (payload, Bulk.Create);
		boolean first = true;
		if (!Json.isNullOrEmpty (create)) {
			Iterator<String> entities = create.keys ();
			while (entities.hasNext ()) {
				String entity = entities.next ();
				JsonArray createItems = Json.getArray (create, entity);
				for (int i = 0; i < createItems.size (); i++) {
					if (!first) {
						sPayload.append (Lang.ENDLN);
					}
					JsonObject item = (JsonObject)createItems.get (i);
					JsonObject document = (JsonObject)new JsonObject ().set (
						Internal.Elk.Operations.Index,
						new JsonObject ()
							.set (Internal.Elk.Index, index)
							.set (Internal.Elk.Type, entity)
							.set (Internal.Elk.Id, Json.getString (item, Internal.Id))
					);
					sPayload.append (document.toString (0, true)).append (Lang.ENDLN);
					
					item.remove (Internal.Id);
					
					sPayload.append (item.toString (0, true));
					
					first = false;
				}
			}
		}
		JsonObject 	update 		= Json.getObject (payload, Bulk.Update);
		if (!Json.isNullOrEmpty (update)) {
			Iterator<String> entities = update.keys ();
			while (entities.hasNext ()) {
				String entity = entities.next ();
				JsonArray updateItems = Json.getArray (update, entity);
				for (int i = 0; i < updateItems.size (); i++) {
					if (!first) {
						sPayload.append (Lang.ENDLN);
					}
					JsonObject item = (JsonObject)updateItems.get (i);
					JsonObject document = (JsonObject)new JsonObject ().set (
						Bulk.Update,
						new JsonObject ()
							.set (Internal.Elk.Index, index)
							.set (Internal.Elk.Type, entity)
							.set (Internal.Elk.Id, Json.getString (item, Internal.Id))
					);
					sPayload.append (document.toString (0, true)).append (Lang.ENDLN);
					
					item.remove (Internal.Id);
					
					sPayload.append (item.toString (0, true));
					
					first = false;
				}
			}
		}
		JsonObject 	delete = Json.getObject (payload, Bulk.Delete);
		if (!Json.isNullOrEmpty (delete)) {
			Iterator<String> entities = delete.keys ();
			while (entities.hasNext ()) {
				String entity = entities.next ();
				JsonArray deleteItems = Json.getArray (delete, entity);
				for (int i = 0; i < deleteItems.size (); i++) {
					if (!first) {
						sPayload.append (Lang.ENDLN);
					}
					JsonObject item = (JsonObject)deleteItems.get (i);
					JsonObject document = (JsonObject)new JsonObject ().set (
						Bulk.Delete,
						new JsonObject ()
							.set (Internal.Elk.Index, index)
							.set (Internal.Elk.Type, entity)
							.set (Internal.Elk.Id, Json.getString (item, Internal.Id))
					);
					sPayload.append (document.toString (0, true));
					
					first = false;
				}
			}
		}
		
		sPayload.append (Lang.ENDLN);
		
		tracer.log (
			Tracer.Level.Info, 
			"Bulk Paylod [{0}]", 
			sPayload.toString ()
		);
		
		JsonObject result = new JsonObject ();
		ElkError error = new ElkError ();
		
		remote.post (
			(JsonObject)new JsonObject ()
				.set (Remote.Spec.Path, Internal.Elk.Bulk)
				.set (Remote.Spec.Headers, 
					new JsonObject ()
						.set (HttpHeaders.CONTENT_TYPE, ContentTypes.XndJson)
				).set (Remote.Spec.Data, new JsonObject ().set (Remote.Spec.Body, sPayload.toString ()))
				.set (Remote.Spec.Serializer, Serializer.Name.json),
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					error.set (code, message);
				}
				@Override
				public void onDone (int code, Object data) {
					if (data != null) {
						result.putAll ((JsonObject)data);
					}
				}
			}
		);
		
		sPayload.setLength (0);
		
		if (error.happened ()) {
			throw new IndexerException ("Error occured while calling Indexer: Code=" + error.code + ", Message: " + error.message);
		}
		
		return result;
	}
	
	private static String utc () {
		SimpleDateFormat formatter = new SimpleDateFormat (DATE_FORMAT);
		formatter.setTimeZone (Lang.UTC_TZ);
		return formatter.format (Lang.utcTime ());
	}
	
}
	
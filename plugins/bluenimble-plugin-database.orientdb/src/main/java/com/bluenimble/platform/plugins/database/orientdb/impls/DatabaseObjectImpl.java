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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.DatabaseObjectSerializer;
import com.bluenimble.platform.db.DatabaseObjectSerializer.Fields;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.orientechnologies.orient.core.record.impl.ODocument;

import jdk.nashorn.internal.runtime.Undefined;

@SuppressWarnings("restriction")
public class DatabaseObjectImpl implements DatabaseObject {

	private static final long serialVersionUID = 3836351496281551423L;
	
	public static final String []	MinimalFields 	= new String [] { Database.Fields.Id, Database.Fields.Timestamp };
	public static final Object 		Null 			= new Object ();

	private Database 	database;
	ODocument 			document;
	
	public DatabaseObjectImpl (Database database, String name) {
		this (database, new ODocument (name));
		useDefaultFields (true);
	}

	public DatabaseObjectImpl (Database database, ODocument document) {
		this.database = database;
		this.document = document;
	}

	@Override
	public void useDefaultFields (boolean useDefaultFields) {
		if (useDefaultFields) {
			setDefaults ();
		} else if (document.getIdentity () == null || !document.getIdentity ().isPersistent ()) {
			unsetDefaults ();
		}
	}

	@Override
	public String name () {
		return document.getClassName ();
	}

	@Override
	public void setId (Object id) {
		document.field (Database.Fields.Id, id);
	}

	@Override
	public Object getId () {
		return document.field (Database.Fields.Id);
	}

	@Override
	public Date getTimestamp () {
		return document.field (Database.Fields.Timestamp);
	}

	@Override
	public void set (String key, Object value) throws DatabaseException {
		if (Lang.isNullOrEmpty (key)) {
			return;
		}

		if (Database.Fields.Entity.equals (key) && value != null) {
			document.setClassName (String.valueOf (value));
			return;
		}

		if (value == null || value.equals (Null) || Undefined.class.equals (value.getClass ())) {
			document.removeField (key);
			return;
		}
		
		if (DatabaseObject.class.isAssignableFrom (value.getClass ())) {
			value = ((DatabaseObjectImpl)value).document;
		} else if (value instanceof LocalDateTime) {
			LocalDateTime ldt = ((LocalDateTime)value);
			value = Date.from (ldt.atZone (ZoneId.systemDefault ()).toInstant ());
		} else if (value instanceof JsonObject) {
			JsonObject child = (JsonObject)value;
			if (child.containsKey (Database.Fields.Entity)) {
				String cEntityName = Json.getString (child, Database.Fields.Entity);
				DatabaseObjectImpl childEntity = null;
				if (child.containsKey (Database.Fields.Id)) {
					childEntity = (DatabaseObjectImpl)database.get (cEntityName, child.get (Database.Fields.Id));
				} 
				if (childEntity == null) {
					childEntity = (DatabaseObjectImpl)database.create (cEntityName);
				} else {
					// remove entity and id
					child.remove (Database.Fields.Entity);
					child.remove (Database.Fields.Id);
				}
				childEntity.load (child);
				value = childEntity.document;
			}
		} else if (List.class.isAssignableFrom (value.getClass ())) {
			@SuppressWarnings("unchecked")
			List<Object> children = (List<Object>)value;
			if (!children.isEmpty ()) {
				List<Object> childDocs = new ArrayList<Object> ();
				for (Object child : children) {
					if (child != null && DatabaseObject.class.isAssignableFrom (child.getClass ())) {
						childDocs.add (((DatabaseObjectImpl)child).document);
					} else {
						childDocs.add (child);
					}
				}
				value = childDocs;
			} else {
				value = null;
			}
		}		
		document.field (key, value);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object get (String key) {

		Object v = document.field (key);
		if (v == null) {
			return null;
		}
		
		if (Map.class.isAssignableFrom (v.getClass ()) && !(v instanceof JsonObject)) {
			return new JsonObject ((Map)v, true);
		} else if (ODocument.class.isAssignableFrom (v.getClass ())) {
			return new DatabaseObjectImpl (database, (ODocument)v);
		} else if (List.class.isAssignableFrom (v.getClass ())) {
			List<Object> objects = (List<Object>)v;
			if (objects.isEmpty ()) {
				return null;
			}
			if (ODocument.class.isAssignableFrom (objects.get (0).getClass ())) {
				return new DatabaseObjectList (database, objects);
			}
			return new JsonArray (objects);
		}
		return v;
	}
	

	@Override
	public void remove (String key) {
		document.removeField (key);
	}

	@Override
	public void load (JsonObject data) throws DatabaseException {
		if (data == null) {
			return;
		}
		
		data.shrink ();
		
		if (Json.isNullOrEmpty (data)) {
			return;
		}
		
		Iterator<String> keys = data.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			Object value = data.get (key);
			
			if (value == null || value.equals (Null) || Undefined.class.equals (value.getClass ())) {
				document.removeField (key);
				continue;
			}
			
			set (key, data.get (key));
		}
	}

	@Override
	public void clear () {
		document.clear ();
		document.detach ();
	}

	@Override
	public Iterator<String> keys () {
		final String [] fields = document.fieldNames ();
		if (fields == null) {
			return null;
		}
		
		return new Iterator<String> () {
			int index = -1;
			@Override
			public boolean hasNext () {
				index++;
				return index < fields.length;
			}
			@Override
			public String next () {
				return fields [index];
			}
			@Override
			public void remove () {
				throw new UnsupportedOperationException ("Iterator.remove not supported");
			}
		};
	}

	@Override
	public JsonObject toJson (DatabaseObjectSerializer serializer) {
		return toJson (document, serializer, 0);
	}
	
	@Override
	public void delete () throws DatabaseException {
		try {
			document.delete ();
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
	}

	@Override
	public boolean has (String key) {
		return document.containsField (key);
	}

	@Override
	public void save () throws DatabaseException {
		try {
			document.save ();
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
	}
	
	private void setDefaults () {
		setId (Lang.rand ());
		try {
			set (Database.Fields.Timestamp, new Date ());
		} catch (DatabaseException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}
	
	private void unsetDefaults () {
		remove (Database.Fields.Id);
		remove (Database.Fields.Timestamp);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JsonObject toJson (ODocument doc, DatabaseObjectSerializer serializer, int level) {
		if (serializer == null) {
			serializer = DatabaseObjectSerializer.Default;
		}

		String type = document.getClassName ();
		
		Fields fields = serializer.fields (level);

		if (fields == null || Fields.None.equals (fields)) {
			return null;
		}
		
		String [] fieldNames = null;
		if (Fields.All.equals (fields)) {
			fieldNames = doc.fieldNames ();
		} else {
			fieldNames = MinimalFields;
		}
		
		JsonObject json = serializer.create (type, level);
		
		if (json == null) {
			return null;
		}
		
		for (String f : fieldNames) {
			Object v = doc.field (f);
			if (v == null) {
				continue;
			}
			
			if (v instanceof Date) {
				v = Lang.toUTC ((Date)v);
			} else if (v instanceof Map) {
				v = new JsonObject ((Map<String, Object>)v, true);
			} else if (v instanceof List) {
				List<Object> list = (List<Object>)v;
				if (list.isEmpty ()) {
					continue;
				}
				JsonArray arr = new JsonArray ();
				for (Object o : list) {
					if (o == null) {
						continue;
					}
					if (o instanceof ODocument) {
						arr.add (toJson ((ODocument)o, serializer, level + 1));
					} else {
						if (o instanceof Date) {
							arr.add (Lang.toUTC ((Date)o));
						} else if (Map.class.isAssignableFrom (o.getClass ())) {
							arr.add (new JsonObject ((Map)o, true));
						} else {
							arr.add (o);
						}
					}
				}
				v = arr;
			} else if (v instanceof ODocument) {
				v = toJson ((ODocument)v, serializer, level + 1);
			}
			
			serializer.set (type, json, f, v);
			
		}
		
		return json;
	}

}

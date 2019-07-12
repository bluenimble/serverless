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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.BeanSchema;
import com.bluenimble.platform.reflect.beans.BeanSchema.FetchStrategy;
import com.bluenimble.platform.reflect.beans.BeanSerializer;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

import jdk.nashorn.internal.runtime.Undefined;

@SuppressWarnings("restriction")
public class DatabaseObjectImpl implements DatabaseObject {

	private static final long serialVersionUID = 3836351496281551423L;
	
	public static final String []	MinimalFields 	= new String [] { Database.Fields.Id, Database.Fields.Timestamp };

	public static final String 		ObjectIdKey			= "_id";
	public static final String 		ObjectEntityKey		= "_entity";
	public static final String 		One2ManyLinkKey		= "_o2m_links";
	
	public static final String 		SetKey				= "$set";
	public static final String 		UnSetKey			= "$unset";

	private String 					entity;
	boolean							persistent;
	boolean							partial;
	
	MongoDatabaseImpl				db;
	
	Document 						document;
	
	Document 						update;
	
	Set<String>						refs;
	
	public DatabaseObjectImpl (MongoDatabaseImpl db, String entity) {
		this.db 			= db;
		this.entity 		= entity;
		this.document 		= new Document ();
		useDefaultFields (true);
	}

	public DatabaseObjectImpl (MongoDatabaseImpl db, String entity, Document document) {
		this.db 		= db;
		this.entity 	= entity;
		this.document (document);
	}

	public DatabaseObjectImpl (MongoDatabaseImpl db, String entity, Object id, boolean partial) {
		this.db 			= db;
		this.entity 		= entity;
		this.document 		= new Document ();
		setId (id);
		this.partial 		= partial;
	}

	@Override
	public void useDefaultFields (boolean useDefaultFields) {
		if (useDefaultFields) {
			setDefaults ();
		} else if (persistent) {
			unsetDefaults ();
		}
	}

	@Override
	public String entity () {
		return entity;
	}

	@Override
	public void setId (Object id) {
		if (id == null) {
			return;
		}
		if (id instanceof String && ObjectId.isValid ((String)id)) {
			document.append (ObjectIdKey, new ObjectId ((String)id));
			return;
		}
		document.append (ObjectIdKey, id);
	}

	@Override
	public Object getId () {
		Object oid = _getId ();
		if (oid instanceof ObjectId) {
			return oid.toString ();
		}
		return oid;
	}

	@Override
	public Date getTimestamp () {
		return (Date)document.get (Database.Fields.Timestamp);
	}

	@Override
	public void set (String key, Object value) throws DatabaseException {
		if (Lang.isNullOrEmpty (key)) {
			return;
		}

		if (Database.Fields.Entity.equals (key) && value != null) {
			entity = String.valueOf (value);
			return;
		}
		
		if (Database.Fields.Id.equals (key) && value != null) {
			setId (value);
			return;
		}
		
		if (value == null || value.equals (Lang.Null) || Undefined.class.equals (value.getClass ())) {
			remove (key);
			return;
		}
		
		if (DatabaseObject.class.isAssignableFrom (value.getClass ())) {
			if (refs == null) {
				refs = new HashSet<String> ();
			}
			if (!refs.contains (key)) {
				refs.add (key);
			}
		} else if (value instanceof LocalDateTime) {
			LocalDateTime ldt = ((LocalDateTime)value);
			value = Date.from (ldt.atZone (ZoneId.systemDefault ()).toInstant ());
		} else if (value instanceof JsonObject) {
			JsonObject child = (JsonObject)value;
			if (child.containsKey (Database.Fields.Entity)) {
				String cEntityName = db.entity (Json.getString (child, Database.Fields.Entity));
				
				DatabaseObjectImpl childEntity = null;
				if (child.containsKey (Database.Fields.Id)) {
					childEntity = (DatabaseObjectImpl)db.get (cEntityName, child.get (Database.Fields.Id));
				} 
				
				if (childEntity == null) {
					childEntity = (DatabaseObjectImpl)db.create (cEntityName);
				} else {
					// remove id
					child.remove (Database.Fields.Id);
				}
				
				child.remove (Database.Fields.Entity);
				
				childEntity.load (child);
				
				// call set and return
				set (key, childEntity);
				return;
				
			}
		// NEED REVIEW: if it's a list	
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
		
		document.append (key, value);
		
		if (!key.equals (Database.Fields.Timestamp)) {
			markForUpdate (key, value);
		}
		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object get (String key) {

		if (Database.Fields.Id.equals (key))  {
			return getId ();
		}
		
		if (partial) {
			refresh ();
		}
		
		Object v = document.get (key);
		if (v == null) {
			return null;
		}
		
		Object refValue = getSetRelationship (key, v);
		if (refValue != null) {
			return refValue;
		}
		
		if (Map.class.isAssignableFrom (v.getClass ()) && !(v instanceof JsonObject)) {
			return new JsonObject ((Map)v, true);
		} else if (List.class.isAssignableFrom (v.getClass ())) {
			List<Object> objects = (List<Object>)v;
			if (objects.isEmpty ()) {
				return null;
			}
			return new JsonArray (objects);
		}
		
		return v;
	}
	
	public void document (Document document) {
		this.document = document;
		this.persistent = true;
	}

	Object _getId () {
		return document.get (ObjectIdKey);
	}

	private Object getSetRelationship (String key, Object v) {
		
		if (!Document.class.isAssignableFrom (v.getClass ())) {
			return null;
		}
		
		Document ref = (Document)v;
		
		Object one2ManyLink = ref.get (One2ManyLinkKey);
		
		if (one2ManyLink != null) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			DatabaseObjectList list = new DatabaseObjectList (db, (List<Document>)one2ManyLink);
			try {
				set (key, list);
			} catch (DatabaseException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
			return list;
		} else {
			String entity 	= ref.getString (ObjectEntityKey);
			Object id 		= ref.get (ObjectIdKey);
			
			if (entity == null || id == null) {
				return null;
			}
			
			DatabaseObjectImpl refObject = new DatabaseObjectImpl (db, entity, id, true);
			refObject.persistent = true;
			try {
				// refObject.set (key, ref.get (Database.Fields.Timestamp));
				set (key, refObject);
			} catch (DatabaseException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
			return refObject;
		} 
		
	}
	
	private void refresh () {
		document = db._get (entity, _getId ());
		partial = false;
	}
	
	private void markForUpdate (String key, Object value) {
		
		// if not persistent, return
		if (!persistent) {
			return;
		}
		
		if (update == null) {
			update = new Document ();
		}

		Document set = (Document)update.get (SetKey);
		if (set == null) {
			set = new Document ();
			update.put (SetKey, set);
		}
		
		set.put (key, value);
	}

	@Override
	public void remove (String key) {
		
		if (Lang.isNullOrEmpty (key)) {
			return;
		}
		
		document.remove (key);
		
		if (refs != null && refs.contains (key)) {
			refs.remove (key);
		}
		
		// if persistent
		if (persistent) {
			if (update == null) {
				update = new Document ();
			}
		}

		if (update != null) {
			Document unset = (Document)update.get (UnSetKey);
			if (unset == null) {
				unset = new Document ();
				update.put (UnSetKey, unset);
			}
			unset.put (key, Lang.BLANK);
		}
		
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
			set (key, data.get (key));
		}
	}

	@Override
	public void clear () {
		document.clear ();
		if (update != null) {
			update.clear ();
		}
		update = null;
		
		if (refs != null) {
			refs.clear ();
		}
		refs = null;
	}

	@Override
	public Iterator<String> keys () {
		Set<String> keys = document.keySet ();
		if (keys == null || keys.isEmpty ()) {
			return null;
		}
		final String [] fields = keys.toArray (new String [keys.size ()]); 
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
	public JsonObject toJson (BeanSerializer serializer) {
		if (serializer == null) {
			serializer = DefaultBeanSerializer.Default;
		}
		return toJson (this, serializer, serializer.schema (), 0, true);
	}
	
	@Override
	public void delete () throws DatabaseException {
		db.delete (entity, _getId ());
	}

	@Override
	public boolean has (String key) {
		return document.containsKey (key);
	}

	@Override
	public void save () throws DatabaseException {
		
		// if no changes to persistent object
		if (persistent && (update == null || update.isEmpty ())) {
			return;
		}
		
		// save refs first
		if (refs != null) {
			for (String ref : refs) {
				DatabaseObjectImpl refObject = ((DatabaseObjectImpl)get (ref));
				refObject.save ();
				
				Document dRef = new Document ()
					.append (ObjectEntityKey, refObject.entity ())
					.append (ObjectIdKey, refObject._getId ());
				
				if (persistent) {
					markForUpdate (ref, dRef);
				} else {
					document.append (ref, dRef);
				}
			}
		}
		
		// save
		if (persistent) {
			if (update != null && !update.isEmpty ()) {
				if (db.session == null) {
					db.getInternal ().getCollection (entity).updateOne (eq (ObjectIdKey, _getId ()), update);
				} else {
					db.getInternal ().getCollection (entity).updateOne (db.session, eq (ObjectIdKey, _getId ()), update);
				}
				update.clear ();
			}
		} else {
			if (db.session == null) {
				db.getInternal ().getCollection (entity).insertOne (document);
			} else {
				db.getInternal ().getCollection (entity).insertOne (db.session, document);
			}
			persistent = true;
		}
		
		// restore 1-1 refs
		if (refs != null) {
			for (String ref : refs) {
				DatabaseObject refObject = ((DatabaseObject)get (ref));
				document.append (ref, refObject);
			}
		}
		
	}
	
	private void setDefaults () {
		try {
			set (Database.Fields.Timestamp, new Date ());
		} catch (DatabaseException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}
	
	private void unsetDefaults () {
		remove (Database.Fields.Timestamp);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JsonObject toJson (DatabaseObjectImpl dbo, BeanSerializer serializer, BeanSchema schema, int level, boolean refresh) {
		if (schema == null) {
			return null;
		}

		if (serializer == null) {
			serializer = BeanSerializer.Default;
		}

		String entity = dbo.entity ();
		
		Set<String> keys = dbo.document.keySet ();
		if (keys == null || keys.isEmpty ()) {
			return null;
		}
		
		// If object is partial and more than minimal fields are requested
		if (dbo.partial && !schema.fetchStrategy ().equals (FetchStrategy.minimal) && refresh) {
			dbo.refresh ();
			keys = dbo.document.keySet ();
		}

		Set<String> fields = schema.fields (keys);
		if (fields == null || fields.isEmpty ()) {
			return null;
		}
		
		JsonObject json = serializer.create (entity, level);
		
		if (json == null) {
			return null;
		}
		
		for (String f : fields) {
			
			Object v = dbo.get (f);
			if (v == null) {
				continue;
			}
			
			if (ObjectIdKey.equals (f)) {
				f = Database.Fields.Id;
			}
			
			if (v instanceof Date) {
				v = Lang.toUTC ((Date)v);
			} else if (v instanceof Map && !(v instanceof JsonObject)) {
				v = new JsonObject ((Map<String, Object>)v, true);
			} else if (v instanceof DatabaseObjectImpl) {
				if (BeanSchema.FetchStrategy.simple.equals (schema.fetchStrategy ())) {
					continue;
				}
				v = toJson (((DatabaseObjectImpl)v), serializer, schema.schema (level, f), level + 1, true);
			} else if (v instanceof DatabaseObjectList) {
				if (BeanSchema.FetchStrategy.simple.equals (schema.fetchStrategy ())) {
					continue;
				}
				List<Object> list = (List<Object>)v;
				if (list.isEmpty ()) {
					continue;
				}
				JsonArray arr = new JsonArray ();
				for (Object o : list) {
					if (o == null) {
						continue;
					}
					if (o instanceof DatabaseObjectImpl) {
						arr.add (toJson (((DatabaseObjectImpl)o), serializer, schema.schema (level, f), level + 1, true));
					} else {
						if (o instanceof Date) {
							arr.add (Lang.toUTC ((Date)o));
						} else if (Map.class.isAssignableFrom (o.getClass ())) {
							arr.add (new JsonObject ((Map)o, true));
						} else if (List.class.isAssignableFrom (o.getClass ())) {
							arr.add (new JsonArray ((List<Object>)o));
						} else {
							arr.add (o);
						}
					}
				}
				v = arr;
			} 
			
			serializer.set (entity, json, f, v);
			
		}
		
		return json;
	}
	
	public String toString () {
		return describe ().toString (2, true);
	}
	
	private JsonObject describe () {
		JsonObject o = new JsonObject ();
		o.set ("entity", entity);
		o.set ("persistent", persistent);
		o.set ("partial", partial);
		o.set ("document", document.hashCode ());
		o.set ("anyUpdates", update == null ? false : !update.isEmpty ());
		
		JsonObject data = new JsonObject ();
		o.set ("data", data);
		
		Iterator<String> keys = keys ();
		if (keys == null) {
			return o;
		}
		
		while (keys.hasNext ()) {
			String key = keys.next ();
			Object value = document.get (key);
			if (value instanceof DatabaseObjectImpl) {
				data.set (key, ((DatabaseObjectImpl)value).describe ());
			} else {
				if (key.equals (ObjectIdKey)) {
					data.set (key, value.getClass ().getSimpleName () + "(" + value.toString () + ")");
				} else {
					data.set (key, value.getClass ().getSimpleName ());
				}
			}
		}
		
		return o;
		
	}

}

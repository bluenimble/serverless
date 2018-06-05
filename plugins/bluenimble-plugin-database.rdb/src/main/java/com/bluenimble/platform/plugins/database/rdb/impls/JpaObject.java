package com.bluenimble.platform.plugins.database.rdb.impls;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.BeanMetadata;
import com.bluenimble.platform.reflect.beans.BeanSerializer;
import com.bluenimble.platform.reflect.beans.BeanSerializer.Fields;

public class JpaObject implements DatabaseObject {

	private static final long serialVersionUID = 8492111603149116104L;
	
	private JpaDatabase 	database;
			Object 			bean;
	private BeanMetadata	metadata;
	
	public JpaObject (JpaDatabase database, Class<?> type) {
		this.database = database;
		this.metadata = database.metadata (type);
	}
	
	public JpaObject (JpaDatabase database, Object bean) {
		this (database, bean.getClass ());
		this.bean = bean;
	}
	
	@Override
	public void useDefaultFields (boolean useDefaultFields) {
		
	}

	@Override
	public String entity () {
		return bean.getClass ().getSimpleName ();
	}

	@Override
	public Object getId () {
		if (!has (Database.Fields.Id)) {
			return null;
		}
		return get (Database.Fields.Id);
	}

	@Override
	public void setId (Object id) {
		if (!has (Database.Fields.Id)) {
			return;
		}
		try {
			set (Database.Fields.Id, id);
		} catch (DatabaseException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}

	@Override
	public Date getTimestamp () {
		if (!has (Database.Fields.Timestamp)) {
			return null;
		}
		return (Date)get (Database.Fields.Timestamp);
	}

	@Override
	public void set (String key, Object value) throws DatabaseException {
		
		// if jsonobject, check for ref entity
		// if list or property is OneToMany/MamyToMany
		
		try {
			metadata.set (bean, key, value);
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	}

	@Override
	public Object get (String key) {
		try {
			return metadata.get (bean, key);
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
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
	public void remove (String key) {
		try {
			set (key, null);
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	}

	@Override
	public void clear () {
		try {
			for (String key : metadata.allFields ()) {
				set (key, null);
			}
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	}

	@Override
	public Iterator<String> keys () {
		return Arrays.asList (metadata.allFields ()).iterator ();
	}

	public JsonObject toJson (BeanSerializer serializer) {
		return toJson (bean, metadata, serializer, 0);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JsonObject toJson (Object bean, BeanMetadata metadata, BeanSerializer serializer, int level) {
		
		if (bean == null || metadata == null || metadata.isEmpty ()) {
			return null;
		}
		
		if (serializer == null) {
			serializer = BeanSerializer.Default;
		}

		String entity = bean.getClass ().getSimpleName ();
		
		Fields fields = serializer.fields (level);

		if (fields == null || Fields.None.equals (fields)) {
			return null;
		}
		
		JsonObject json = serializer.create (entity, level);
		
		if (json == null) {
			return null;
		}
		
		String [] fieldNames = null;
		
		if (Fields.All.equals (fields)) {
			fieldNames = metadata.allFields ();
		} else {
			fieldNames = metadata.minimalFields ();
		}
		
		if (fieldNames == null || fieldNames.length == 0) {
			return null;
		}
		
		for (String f : fieldNames) {
			Object v;
			try {
				v = metadata.get (bean, f);
			} catch (Exception ex) {
				throw new RuntimeException (ex.getMessage (), ex);
			}
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
					if (database.isEntity (o)) {
						arr.add (toJson (o, database.metadata (o.getClass ()), serializer, level + 1));
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
			} else if (database.isEntity (v)) {
				v = toJson (v, database.metadata (v.getClass ()), serializer, level + 1);
			}
			
			serializer.set (entity, json, f, v);
			
		}
		
		return json;
	}
	
	@Override
	public boolean has (String key) {
		return metadata.has (key);
	}

	@Override
	public void save () throws DatabaseException {
		try {
			if (!database.entityManager.contains (bean)) {
				database.entityManager.persist (bean);
			} else {
				database.entityManager.merge (bean);
			}
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
	}

	@Override
	public void delete () throws DatabaseException {
		try {
			database.entityManager.remove (bean);
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
	}

}

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
package com.bluenimble.platform.reflect.beans.impls;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.BeanSchema;

public class JsonBeanSchema implements BeanSchema {
	
	interface Spec {
		String Fields = "_fields";
	}
	
	private static final String 		Minimal 			= "minimal";
	private static final String 		All 				= "all";
	private static final String 		XAll 				= "xall";
	private static final Set<String> 	AllowedStrategies 	= new HashSet<String> (Arrays.asList (new String [] { "minimal", "simple", "all", "xall"}));
	
	protected JsonObject 	schema;
	protected boolean 		simpleRefs;
	protected FetchStrategy fetchStrategy;
	
	protected JsonBeanSchema () {
	}
	
	public JsonBeanSchema (JsonObject schema) {
		create (this, schema);
	}

	@Override
	public BeanSchema schema (int level, String field) {
		if (Lang.isNullOrEmpty (field)) {
			return null;
		}
		Object o = schema.get (field);
		if (o == null) {
			return simpleRefs ? BeanSchema.Simple : (this.fetchStrategy == FetchStrategy.all ? BeanSchema.Minimal : null);
		}
		if (o instanceof JsonBeanSchema) {
			return (JsonBeanSchema)o;
		}
		return BeanSchema.Minimal;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> fields (Set<String> available) {
		if (Json.isNullOrEmpty (schema)) {
			return null;
		}
		
		Iterator<String> sKeys = schema.keys ();
		
		switch (fetchStrategy ()) {
			case simple:
				while (sKeys.hasNext ()) {
					String sKey = sKeys.next ();
					if (sKey.equals (Spec.Fields)) {
						continue;
					}
					Object value = schema.get (sKey);
					if (value instanceof Boolean && !((Boolean)value)) {
						available.remove (sKey);
					}
				}
				return available;
			case all:
				return available;
			case listed:
				while (sKeys.hasNext ()) {
					String sKey = sKeys.next ();
					if (sKey.equals (Spec.Fields)) {
						continue;
					}
					Object value = schema.get (sKey);
					if (value instanceof Boolean && !((Boolean)value)) {
						available.remove (sKey);
					}
				}
				return available;
			case minimal:
				return MinimalFields;
			case extended:
				Set<String> fields = new HashSet<String> ();
				fields.addAll (MinimalFields);
				if (schema.count () == 1) {
					return fields;
				}
				while (sKeys.hasNext ()) {
					String sKey = sKeys.next ();
					if (sKey.equals (Spec.Fields)) {
						continue;
					}
					Object value = schema.get (sKey);
					if (!(value instanceof Boolean) || (Boolean)value) {
						fields.add (sKey);
					} else {
						fields.remove (sKey);
					}
				}
				return fields;
			default:
				return schema.keySet ();
		}
	}
	
	@Override
	public FetchStrategy fetchStrategy () {
		return fetchStrategy;
	}
	
	@SuppressWarnings("unchecked")
	private void setFetchStrategy () {
		if (Json.isNullOrEmpty (schema)) {
			fetchStrategy = FetchStrategy.minimal;
			return;
		}
		String fs = Json.getString (schema, Spec.Fields);
		if (Lang.isNullOrEmpty (fs)) {
			if (isOnlyIdAndTimestamp (schema.keySet ())) {
				fetchStrategy = FetchStrategy.minimal;
				return;
			} 
			fetchStrategy = FetchStrategy.listed;
			return;
		}
		
		fs = fs.toLowerCase ();
		
		FetchStrategy strategy = null;
		
		if (!AllowedStrategies.contains (fs)) {
			strategy = FetchStrategy.minimal;
		} else {
			if (XAll.equals (fs)) {
				simpleRefs = true;
				fs = All;
			}
			strategy = FetchStrategy.valueOf (fs);
		}
		
		if (schema.size () == 1) {
			fetchStrategy = strategy;
			return;
		}
		
		// if it's all, excluding fields, return listed
		if (strategy.equals (FetchStrategy.all)) {
			fetchStrategy = FetchStrategy.listed;
			return;
		}
		
		// if it's minimal, including adt. fields, return extended
		if (strategy.equals (FetchStrategy.minimal)) {
			fetchStrategy = FetchStrategy.extended;
			return;
		}
		fetchStrategy = strategy;
	}

	private static void create (JsonBeanSchema jbs, JsonObject schema) {
		Object fields = schema.get (Spec.Fields);
		if (fields != null && fields instanceof JsonArray) {
			JsonArray aFields = (JsonArray)fields;
			for (int i = 0; i < aFields.count (); i++) {
				String sField = String.valueOf (aFields.get (i)).trim ();
				boolean include = true;
				if (sField.startsWith (Lang.XMARK)) {
					sField = sField.substring (1);
					include = false;
				}
				schema.set (sField, include);
			}
			schema.merge (schema);
			schema.remove (Spec.Fields);
		}
		
		fields = schema.get (Spec.Fields);
		// if enumerated, compose it
		if (fields == null) {
			schema.set (Spec.Fields, Minimal);
			if (!schema.containsKey (Database.Fields.Timestamp)) {
				schema.set (Database.Fields.Timestamp, false);
			}
			if (!schema.containsKey (Database.Fields.Id)) {
				schema.set (Database.Fields.Id, false);
			}
		}
		
		jbs.schema = schema;
		jbs.setFetchStrategy ();
		
		Iterator<String> keys = schema.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			Object o = schema.get (key);
			if (o instanceof JsonObject) {
				JsonBeanSchema cjbs = new JsonBeanSchema ();
				create (cjbs, (JsonObject)o);
				schema.set (key, cjbs);
			}
		}
	}

	private boolean isOnlyIdAndTimestamp (Set<String> keys) {
		if (keys.size () > 2) {
			return false;
		}
		if (keys.size () == 1) {
			return keys.contains (Database.Fields.Id) || keys.contains (Database.Fields.Timestamp);
		}
		return keys.contains (Database.Fields.Id) && keys.contains (Database.Fields.Timestamp);
	}
	
}

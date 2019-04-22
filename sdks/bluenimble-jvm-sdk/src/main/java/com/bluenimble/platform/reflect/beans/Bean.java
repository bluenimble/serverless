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
package com.bluenimble.platform.reflect.beans;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

public class Bean implements Serializable {

	private static final long serialVersionUID = 5019655125792300369L;
	
	protected BeanMetadata metadata;
	
	protected Bean () {
		metadata = new BeanMetadata (this.getClass (), new BeanMetadata.MinimalFieldsSelector () {
			@Override
			public boolean select (PropertyDescriptor pd) {
				return true;
			}
		});
	}
	
	public void set (String key, Object value) throws Exception {
		metadata.set (this, key, value);
	}

	public Object get (String key) throws Exception {
		return metadata.get (this, key);
	}

	public void load (JsonObject data) throws Exception {
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

	public JsonObject toJson (BeanSerializer serializer) throws Exception {
		if (serializer == null) {
			serializer = DefaultBeanSerializer.Default;
		}
		return toJson (this, metadata, serializer, serializer.schema (), 0);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JsonObject toJson (Bean bean, BeanMetadata metadata, BeanSerializer serializer, BeanSchema schema, int level) throws Exception {
		
		if (bean == null || metadata == null || metadata.isEmpty ()) {
			return null;
		}
		
		if (serializer == null) {
			serializer = BeanSerializer.Default;
		}

		if (metadata.isEmpty ()) {
			return null;
		}
		
		if (schema == null) {
			return null;
		}
		
		String entity = bean.getClass ().getSimpleName ();
		
		Set<String> fields = schema.fields (metadata.allFields ());
		if (fields == null || fields.isEmpty ()) {
			return null;
		}
		
		JsonObject json = serializer.create (entity, level);
		
		if (json == null) {
			return null;
		}
		
		for (String f : fields) {
			Object v = metadata.get (bean, f);
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
					if (Bean.class.isAssignableFrom (o.getClass ())) {
						arr.add (toJson ((Bean)o, ((Bean)o).metadata, serializer, schema.schema (level, f), level + 1));
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
			} else if (Bean.class.isAssignableFrom (v.getClass ())) {
				v = toJson ((Bean)v, ((Bean)v).metadata, serializer, schema.schema (level, f), level + 1);
			}
			
			serializer.set (entity, json, f, v);
			
		}
		
		return json;
	}

}

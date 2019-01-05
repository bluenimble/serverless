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
package com.bluenimble.platform.templating.impls.converters;

import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Undefined;

@SuppressWarnings("restriction")
public class JsValueConverter {
	
	// private static final String Clazz = "class";
	private static final String Proxy = "proxy";
	
	// private static final String LocalDateTime = "LocalDateTime";

	public static Object convert (Object o) {
		if (o == null || o instanceof Undefined) {
			return null;
		}
		if (!(o instanceof ScriptObjectMirror)) {
			return o;
		}
		ScriptObjectMirror som = (ScriptObjectMirror)o;
		if (som.isArray ()) {
			return toArray (som);
		} 
		return toObject (som);
	}
	
	public static JsonArray toArray (ScriptObjectMirror som) {
		JsonArray json = new JsonArray ();
		if (som.isEmpty ()) {
			return json;
		}
		for (Object o : som.values ()) {
			json.add (convert (o));
		}
		return json;
	}
	
	private static Object toObject (ScriptObjectMirror som) {
		Object proxy = som.get (Proxy);
		if (proxy != null && proxy.getClass ().equals (java.time.LocalDateTime.class)) {
			return proxy;
		}
		JsonObject json = new JsonObject ();
		if (som.isEmpty ()) {
			return json;
		}
		
		String [] keys = som.getOwnKeys (true);
		for (String k : keys) {
			json.set (k, convert (som.get (k)));
		}
		return json.resolve ();
	}
	
}

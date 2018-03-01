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
package com.bluenimble.platform.reflect;

import java.lang.reflect.Method;
import java.util.Iterator;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.json.JsonObject;

@SuppressWarnings("rawtypes")
public class BeanUtils {
	
	private static final Class<?> 	[] 	NoTypes 	= new Class [] {};
	private static final Object 	[] 	NoValues 	= new Object [] {};
	
	private static final String 		Dollar		= "$";

	private static final String 		Get 		= "get";
	private static final String 		Is 			= "is";
	private static final String 		Set 		= "set";

	private static final String 		Clazz 		= "class";
	private static final String 		Properties 	= "properties";
	
	private static final String 		Enabled 	= "enabled";
	
	public static Object create (JsonObject definition) throws Exception {
		return create (null, definition);
	}
	
	public static Object create (ClassLoader loader, JsonObject definition) throws Exception {
		return create (loader, definition, null);
	}
	
	public static Object create (ClassLoader loader, JsonObject definition, ClassLoaderRegistry registry) throws Exception {
		if (definition == null) {
			return null;
		}

		if (loader == null) {
			loader = Thread.currentThread ().getContextClassLoader ();
		}

		boolean enabled = Json.getBoolean (definition, Enabled, true);
		
		if (!enabled) {
			return null;
		}
		
		String clazz = Json.getString (definition, Clazz);
		if (clazz == null) {
			throw new Exception ("Property " + Clazz + " not found");
		}
		
		int indexOfColon = clazz.indexOf (Lang.COLON);
		
		if (indexOfColon > 0) {
			String loaderName 	= clazz.substring (0, indexOfColon);
			clazz 		= clazz.substring (indexOfColon + 1);
			if (registry != null) { 
				ClassLoader rLoader		= registry.find (loaderName);
				if (rLoader != null) {
					loader = rLoader;
				}
			}
		}
		
		Object bean = null;
		if (PackageClassLoader.class.isAssignableFrom (loader.getClass ())) {
			PackageClassLoader pcl = (PackageClassLoader)loader;
			if (pcl.hasSynonym (clazz)) {
				System.out.println ("Has Synonym!");
				clazz = pcl.synonym (clazz);
				System.out.println (" is... " + clazz);
			}
			bean = ((PackageClassLoader)loader).lookupObject (clazz);
		}
		
		if (bean == null) {
			bean = loader.loadClass (clazz).newInstance ();
			apply (bean, definition, registry);
		}

		return bean;
		
	}

	public static Object apply (Class<?> clazz, JsonObject definition, ClassLoaderRegistry registry) throws Exception {
		Object o = clazz.newInstance ();
		apply (o, definition, registry);
		return o;
	}
	
	@SuppressWarnings("unchecked")
	public static void apply (Object bean, JsonObject definition, ClassLoaderRegistry registry) throws Exception {
		JsonObject properties = Json.getObject (definition, Properties);
		if (properties == null) {
			return;
		}
		Class beanClass = bean.getClass ();
		Iterator<String> keys = properties.keySet ().iterator ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			if (key.startsWith (Dollar)) {
				continue;
			}
			set (bean, typeOf (beanClass, key), key, properties.get (key), registry);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Class typeOf (Class clazz, String name) throws Exception {
		String mName = name.substring (0, 1).toUpperCase () + name.substring (1);
		Method method = null;
		try {
			method = clazz.getMethod (Get + mName, NoTypes);
		} catch (NoSuchMethodException nsmex) {
			// IGNORE
		}
		
		if (method == null) {
			try {
				method = clazz.getMethod (Is + mName, NoTypes);
			} catch (NoSuchMethodException nsmex) {
				// IGNORE
			}
		}
		if (method == null) {
			throw new NoSuchMethodException (Get + mName + " or " + Is + mName + " not defined for property " + name);
		}
		
		return method.getReturnType ();
	} 

	public static void set (Object bean, Class type, String name, Object value, ClassLoaderRegistry registry) throws Exception {
		
		if (value != null) {
			Class vClass = value.getClass ();
			
			if (vClass.equals (String.class)) {
				value = convert ((String)value, type);
			} else if (vClass.equals (JsonObject.class)) {
				if (!Lang.isNullOrEmpty (Json.getString ((JsonObject)value, Clazz))) {
					value = create (bean.getClass ().getClassLoader (), (JsonObject)value, registry);
				}
			}
			
		}
		
		Method method = bean.getClass ().getMethod (Set + name.substring (0, 1).toUpperCase () + name.substring (1), new Class [] {type});
		
		method.invoke (bean, new Object [] {value});
		
	} 
	
	public static Object get (Object bean, String name) throws Exception {
		return bean.getClass ().getMethod (Get + name.substring (0, 1).toUpperCase () + name.substring (1), NoTypes)
			.invoke (bean, NoValues);
	} 
	
	public static Object convert (String value, Class type) {
		if (type == String.class) {
			return value;
		}
		if (type.equals (Integer.TYPE) || type.equals (Integer.class)) {
			return Integer.valueOf (value);
		} else if (type.equals (Boolean.TYPE) || type.equals (Boolean.class)) {
			return Boolean.valueOf (value);
		} else if (type.equals (Long.TYPE) || type.equals (Long.class)) {
			return Long.valueOf (value);
		}
		return value;
	}

}

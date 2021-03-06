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
package com.bluenimble.platform.plugins.database.rdb;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.utils.InstallUtils;
import com.bluenimble.platform.templating.SimpleVariableResolver;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public class DataSourceVendor {
	
	private static final Set<String> Tokens = new HashSet<String> ();
	static {
		Tokens.add (Lang.COLON); Tokens.add (Lang.SLASH);
	}
	
	private static final String 					Vendor 		= "vendor.json";
	
	private static final String 					Classpath 	= "classpath";
	private static final String 					Template 	= "template";
	private static final String 					Name 		= "name";
	private static final String 					Driver 		= "driver";
	
	private static final DefaultExpressionCompiler 	Compiler = new DefaultExpressionCompiler ();

	private String		name;
	
	private JsonObject 	descriptor;
	private ClassLoader classLoader;
	
	public DataSourceVendor (File home) throws Exception {
		
		this.name = home.getName ();
		
		File vendor = new File (home, Vendor);
		if (!vendor.exists ()) {
			return;
		}
		descriptor = Json.load (vendor);
		
		classLoader = new VendorClassLoader (
			home.getName (), 
			InstallUtils.toUrls (home, Json.getArray (descriptor, Classpath))
		);
		
	}
	
	public String url (String host, int port, String database, String type, File dataFolder) {
		
		if (port <= 0) {
			port = Json.getInteger (descriptor, RdbPlugin.Spec.Port, 0);
		}
		
		String url = Json.getString (descriptor, type == null ? Template : Template + Lang.DOT + type);
		
		final JsonObject data = (JsonObject)new JsonObject ()
				.set (RdbPlugin.Spec.Host, host)
				.set (RdbPlugin.Spec.Port, port)
				.set (RdbPlugin.Spec.Database, database)
				.set (RdbPlugin.Spec.Type, type)
				.set (RdbPlugin.DataFolder, dataFolder.getAbsolutePath ());
		
		VariableResolver vr = new SimpleVariableResolver () {
			private static final long serialVersionUID = -485939153491337463L;

			@Override
			public Object resolve (String namespace, String... property) {
				
				String [] aProperty = new String [property.length];
				
				System.arraycopy (property, 0, aProperty, 0, property.length);
				
				String firstChar = aProperty [0].substring (0, 1);
				if (Tokens.contains (firstChar)) {
					aProperty [0] = aProperty [0].substring (1);
				} else {
					firstChar = Lang.BLANK;
				}
				Object v = Json.find (data, aProperty);
				if (v != null) {
					return firstChar + v;
				}
				return null;
			}
			
		};
		return (String)Compiler.compile (url, null).eval (vr);
	}
	
	public String name () {
		return Json.getString (descriptor, Name, name);
	}
	
	public String driver () {
		return Json.getString (descriptor, Driver);
	}
	
	public ClassLoader classLoader () {
		return classLoader;
	}
	
}

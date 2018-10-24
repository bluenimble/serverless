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
package com.bluenimble.platform.server.plugins.shell;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.impls.ApiImpl;
import com.bluenimble.platform.api.impls.ApiSpaceImpl;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;
import com.bluenimble.platform.shell.OsCommandExecuter;
import com.bluenimble.platform.shell.Shell;
import com.bluenimble.platform.shell.impls.DefaultOsCommandExecuter;
import com.bluenimble.platform.shell.impls.ace.DefaultShell;

public class DefaultShellPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private static final OsCommandExecuter DefaultCommander = new DefaultOsCommandExecuter ();
	
	private static final String ShellSeparator = " && ";
	private static final String EventPrefix = "on";
	
	interface ApiShellParams {
		String Api 			= "API";
		String Home 		= "HOME";
		String UserHome 	= "USER_HOME";
	}
	
	interface Spec {
		String BaseDirectory 	= "baseDirectory";
		String SuccessCodes		= "successCodes";
	}
	
	private String 		feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Shell.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = 3585173809402444745L;
			@Override
			public String id () {
				return null;
			}
			@Override
			public Class<?> type () {
				return Shell.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				RecyclableOsCommandExecuter roce = (RecyclableOsCommandExecuter)space.getRecyclable (createKey (name));
				if (roce == null) {
					return null;
				}
				return new DefaultShell (roce.getBaseDirectory (), roce.getCommandExecuter ());
			}
			@Override
			public String provider () {
				return DefaultShellPlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return DefaultShellPlugin.this;
			}
		});
	}

	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {
		
		if (ApiSpace.class.isAssignableFrom (target.getClass ())) {
			ApiSpace space = (ApiSpace)target;
			switch (event) {
				case Create:
					createClients (space);
					break;
				case AddFeature:
					createClient (space, Json.getObject (space.getFeatures (), feature), (String)args [0], (Boolean)args [1]);
					break;
				case DeleteFeature:
					removeClient (space, (String)args [0]);
					break;
				default:
					break;
			}
		}

		File 		home 		= null;
		String 		namespace 	= null;
		JsonArray 	commands	= null;
		
		if (target instanceof ApiImpl) {
			home 		= ((ApiImpl)target).getHome ();
			namespace 	= ((ApiImpl)target).getNamespace ();
			commands	= Json.getArray (((ApiImpl)target).getRuntime (), EventPrefix + event.name ());
		} else if (target instanceof ApiSpaceImpl) {
			home 		= ((ApiSpaceImpl)target).home ();
			namespace 	= ((ApiSpaceImpl)target).getNamespace ();
			commands	= (JsonArray)((ApiSpaceImpl)target).getRuntime (EventPrefix + event.name ());
		} 
		if (Json.isNullOrEmpty (commands)) {
			return;
		}
		runEventCommands (namespace, home, commands);
	}
	
	private void createClients (ApiSpace space) throws PluginRegistryException {
		
		// create factories
		JsonObject allFeatures = Json.getObject (space.getFeatures (), feature);
		if (Json.isNullOrEmpty (allFeatures)) {
			return;
		}
		
		Iterator<String> keys = allFeatures.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			createClient (space, allFeatures, key, false);
		}
	}
	
	private void createClient (ApiSpace space, JsonObject allFeatures, String name, boolean overwrite) throws PluginRegistryException {
		
		JsonObject feature = Json.getObject (allFeatures, name);
		
		if (!this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
			return;
		}
		
		JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
		
		if (spec == null) {
			return;
		}
		
		String dataSourceKey = createKey (name);
		
		tracer ().log (Tracer.Level.Info, "Create OsCommandExecutor {0}", dataSourceKey);
		
		if (space.containsRecyclable (dataSourceKey)) {
			return;
		}
		
		String userHome = System.getProperty ("user.home");
		
		String baseDirectory = Json.getString (spec, Spec.BaseDirectory, userHome);
		if (baseDirectory.startsWith (Lang.TILDE)) {
			baseDirectory = userHome + baseDirectory.substring (1);
		}
		
		Set<Integer> sSuccessCodes = null;
		
		JsonArray successCodes = Json.getArray (spec, Spec.SuccessCodes);
		if (!Json.isNullOrEmpty (successCodes)) {
			sSuccessCodes = new HashSet<Integer> ();
			for (int i = 0; i < successCodes.count (); i++) {
				sSuccessCodes.add (Integer.valueOf (String.valueOf (successCodes.get (i))));
			}
		}

		RecyclableOsCommandExecuter roce = new RecyclableOsCommandExecuter (new File (baseDirectory), new DefaultOsCommandExecuter (sSuccessCodes));
		
		space.addRecyclable (dataSourceKey, roce);
	}
	
	private void removeClient (ApiSpace space, String featureName) {
		String key = createKey (featureName);
		RecyclableOsCommandExecuter recyclable = (RecyclableOsCommandExecuter)space.getRecyclable (key);
		if (recyclable == null) {
			return;
		}
		// remove from recyclables
		space.removeRecyclable (key);
		// recycle
		recyclable.recycle ();
	}
	
	private String createKey (String name) {
		return feature + Lang.DOT + getNamespace () + Lang.DOT + name;
	}

	class RecyclableOsCommandExecuter implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private File				baseDirectory;
		private OsCommandExecuter 	osCommandExecuter;
		
		public RecyclableOsCommandExecuter (File baseDirectory, OsCommandExecuter osCommandExecuter) {
			this.baseDirectory 		= baseDirectory;
			this.osCommandExecuter 	= osCommandExecuter;
		}
		
		public OsCommandExecuter getCommandExecuter () {
			return osCommandExecuter;
		}
		
		public File getBaseDirectory () {
			return baseDirectory;
		}
		
		@Override
		public void finish (boolean withError) {
		}

		@Override
		public void recycle () {
		}
	}

	private void runEventCommands (String namespace, File home, JsonArray commands) throws PluginRegistryException {
		if (Json.isNullOrEmpty (commands)) {
			return;
		}
		
		JsonObject params = new JsonObject();
		params.set (ApiShellParams.Api, namespace);
		params.set (ApiShellParams.Home, home.getAbsolutePath ());
		params.set (ApiShellParams.UserHome, System.getProperty ("user.home"));
		
		DefaultShell shell = new DefaultShell (home, DefaultCommander);
		
		JsonObject result = shell.run (commands.join (ShellSeparator, false), params);

		if (Json.getInteger (result, Shell.Spec.Code, 0) > 0) {
			throw new PluginRegistryException (Json.getString (result, Shell.Spec.Message));
		}
	}
	
}

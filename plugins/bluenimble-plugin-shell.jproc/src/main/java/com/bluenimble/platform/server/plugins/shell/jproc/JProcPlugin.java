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
package com.bluenimble.platform.server.plugins.shell.jproc;

import java.util.Iterator;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;
import com.bluenimble.platform.shell.Shell;
import com.bluenimble.platform.shell.impls.jproc.JProcShell;

public class JProcPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	interface Spec {
		String BaseDirectory 	= "baseDirectory";
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
				return new JProcShell (roce.getBaseDirectory ());
			}
			@Override
			public String provider () {
				return JProcPlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return JProcPlugin.this;
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
		
		String shellKey = createKey (name);
		
		tracer ().log (Tracer.Level.Info, "Create OsCommandExecutor {0}", shellKey);
		
		if (space.containsRecyclable (shellKey)) {
			return;
		}
		
		String baseDirectory = Json.getString (spec, Spec.BaseDirectory, System.getProperty ("user.home"));
		
		RecyclableOsCommandExecuter roce = new RecyclableOsCommandExecuter (baseDirectory);
		
		space.addRecyclable (shellKey, roce);
		
		feature.set (ApiSpace.Spec.Installed, true);
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

		private String				baseDirectory;
		
		public RecyclableOsCommandExecuter (String baseDirectory) {
			this.baseDirectory 		= baseDirectory;
		}
		
		public String getBaseDirectory () {
			return baseDirectory;
		}
		
		@Override
		public void finish (boolean withError) {
		}

		@Override
		public void recycle () {
		}
	}
	
}

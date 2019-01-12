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
package com.bluenimble.platform.server.plugins.storage.fs;

import java.io.File;
import java.util.Iterator;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;
import com.bluenimble.platform.storage.Storage;
import com.bluenimble.platform.storage.impls.FileSystemStorage;

public class FileSystemStoragePlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private static final int DefaultBuffer = 32 * 1000;

	interface Spec {
		String Mount 	= "mount";
		String Buffer	= "buffer";
	}
	
	private String 	feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Storage.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();

		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = -9012279234275100528L;
			
			@Override
			public String id () {
				return null;
			}
			@Override
			public Class<?> type () {
				return Storage.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				RecyclableMount rm = (RecyclableMount)space.getRecyclable (createKey (name));
				if (rm == null) {
					return null;
				}
				tracer ().log (Level.Info, "New Storage Instance: " + rm.mount ().getAbsolutePath ());
				return new FileSystemStorage (rm.mount (), rm.buffer ());
			}
			@Override
			public String provider () {
				return FileSystemStoragePlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return FileSystemStoragePlugin.this;
			}
		});
	}
	
	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
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
	
	private void createClients (ApiSpace space) {
		JsonObject allFeatures = Json.getObject (space.getFeatures (), feature);
		
		if (Json.isNullOrEmpty (allFeatures)) {
			return;
		}
		
		Iterator<String> keys = allFeatures.keys ();
		while (keys.hasNext ()) {
			createClient (space, allFeatures, keys.next (), false);
		}
	}
	
	private void createClient (ApiSpace space, JsonObject allFeatures, String name, boolean overwrite) {
		
		JsonObject feature = Json.getObject (allFeatures, name);
		
		if (!this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
			return;
		}
		
		JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
		if (spec == null) {
			return;
		}
		
		String mount = Json.getString (spec, Spec.Mount);
		if (Lang.isNullOrEmpty (mount)) {
			return;
		}
		
		if (mount.startsWith (Lang.TILDE)) {
			mount = System.getProperty ("user.home") + Lang.SLASH + mount.substring (1);
		}
		
		File fMount = new File (mount);
		if (!fMount.exists ()) {
			fMount.mkdir ();
		}
		
		if (!fMount.isDirectory ()) {
			return;
		}
		
		tracer ().log (Level.Info, "Mount: " + fMount.getAbsolutePath ());
		
		if (overwrite) {
			removeClient (space, name);
		}
		
		space.addRecyclable (createKey  (name), new RecyclableMount (fMount, Json.getInteger (spec, Spec.Buffer, DefaultBuffer)));
		
		feature.set (ApiSpace.Spec.Installed, true);
	}
	
	private void removeClient (ApiSpace space, String featureName) {
		String key = createKey (featureName);
		RecyclableMount recyclable = (RecyclableMount)space.getRecyclable (key);
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
	
	class RecyclableMount implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;
		
		private File 	mount;
		private int 	buffer;
		
		public RecyclableMount (File mount, int buffer) {
			this.mount 	= mount;
			this.buffer = buffer;
		}
		
		public File mount () {
			return mount;
		}

		public int buffer () {
			return buffer;
		}

		@Override
		public void finish (boolean withError) {
		}

		@Override
		public void recycle () {
		}
	}
	
}

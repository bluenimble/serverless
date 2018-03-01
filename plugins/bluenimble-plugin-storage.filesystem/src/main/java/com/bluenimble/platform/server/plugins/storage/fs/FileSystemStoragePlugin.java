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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiSpace;
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

	private static final String Provider = "bnb-storage";
	
	interface Spec {
		String Mount 	= "mount";
	}
	
	private String 	root;
	private File 	fRoot;
	
	private String 	feature;
	
	private Map<String, String> mounts = new HashMap<String, String> ();
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Storage.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();

		if (!Lang.isNullOrEmpty (root)) {
			fRoot = new File (root);
		}
		if (!fRoot.exists ()) {
			fRoot.mkdirs ();
		}
		
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = -9012279234275100528L;
			
			@Override
			public Class<?> type () {
				return Storage.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				String mount = mounts.get (createStorageKey  (name, space));
				if (mount == null) {
					return null;
				}
				return new FileSystemStorage (mount, new File (fRoot, mount));
			}
			@Override
			public String provider () {
				return Provider;
			}
			@Override
			public Plugin implementor () {
				return FileSystemStoragePlugin.this;
			}
		});
	}

	public void setRoot (String root) {
		this.root = root;
	}
	public String getRoot () {
		return root;
	}
	
	@Override
	public void onEvent (Event event, Object target) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Create:
				createStorage (space);
				break;
			case AddFeature:
				createStorage (space);
				break;
			case DeleteFeature:
				// NOTHING TO BE DONE. Backup? maybe !!!
				
				break;
			default:
				break;
		}
	}
	
	private void createStorage (ApiSpace space) {
		JsonObject storageFeature = Json.getObject (space.getFeatures (), feature);
		if (storageFeature == null || storageFeature.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = storageFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject source = Json.getObject (storageFeature, key);
			
			if (!Provider.equalsIgnoreCase (Json.getString (source, ApiSpace.Features.Provider))) {
				continue;
			}
			
			JsonObject spec = Json.getObject (source, ApiSpace.Features.Spec);
			if (spec == null) {
				continue;
			}
			
			String mount = Json.getString (spec, Spec.Mount);
			if (Lang.isNullOrEmpty (mount)) {
				continue;
			}
			
			File spaceStorage = new File (fRoot, mount);
			if (!spaceStorage.exists ()) {
				spaceStorage.mkdir ();
			}
			
			mounts.put (createStorageKey  (key, space), mount);
		}
	}
	
	private String createStorageKey (String name, ApiSpace space) {
		return feature + Lang.DOT + space.getNamespace () + Lang.DOT + name;
	}
	
}

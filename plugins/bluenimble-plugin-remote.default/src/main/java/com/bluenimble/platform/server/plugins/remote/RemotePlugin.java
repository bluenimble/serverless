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
package com.bluenimble.platform.server.plugins.remote;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.factories.RemoteFactory;
import com.bluenimble.platform.remote.factories.impls.HttpRemoteFactory;
import com.bluenimble.platform.remote.impls.http.HttpRemote;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

public class RemotePlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;

	interface Spec {
		String Protocol 	= "protocol";
	}
	
	enum Protocol {
		http,
		coap,
		ldap,
		ssh
	}
	
	private static final Map<Protocol, RemoteFactory> Factories = new HashMap<Protocol, RemoteFactory>();
	static {
		Factories.put (Protocol.http, new HttpRemoteFactory ());
	}
	
	private Map<String, Remote> remotes = new HashMap<String, Remote> ();
	
	private String 	feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Remote.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}

		feature = aFeature.name ();

		PackageClassLoader pcl = (PackageClassLoader)RemotePlugin.class.getClassLoader ();
		
		pcl.registerObject (Protocol.http.name (), new HttpRemote (null));
		
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = -9012279234275100528L;
			
			@Override
			public Class<?> type () {
				return Remote.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return remotes.get (createRemoteKey (name, space));
			}
			@Override
			public String provider () {
				return RemotePlugin.this.getName ();
			}
			@Override
			public Plugin implementor () {
				return RemotePlugin.this;
			}
		});
	}

	@Override
	public void onEvent (Event event, Object target) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Create:
				createRemote (space);
				break;
			case AddFeature:
				createRemote (space);
				break;
			case DeleteFeature:
				// NOTHING TO BE DONE. Backup? maybe !!!
				
				break;
			default:
				break;
		}
	}
	
	private void createRemote (ApiSpace space) {
		JsonObject storageFeature = Json.getObject (space.getFeatures (), feature);
		if (storageFeature == null || storageFeature.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = storageFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject source = Json.getObject (storageFeature, key);
			
			if (!this.getName ().equalsIgnoreCase (Json.getString (source, ApiSpace.Features.Provider))) {
				continue;
			}
			
			JsonObject spec = Json.getObject (source, ApiSpace.Features.Spec);
			if (spec == null) {
				spec = JsonObject.Blank;
			}
			
			Protocol protocol = null;
			
			try {
				protocol = Protocol.valueOf (Json.getString (spec, Spec.Protocol, Protocol.http.name ()).toLowerCase ());	
			} catch (Exception ex) {
				protocol = Protocol.http;
			}
			
			remotes.put (createRemoteKey  (key, space), Factories.get (protocol).create (space, key, spec));
		}
	}
	
	private String createRemoteKey (String name, ApiSpace space) {
		return feature + Lang.DOT + space.getNamespace () + Lang.DOT + name;
	}
	
}

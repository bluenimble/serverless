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
package com.bluenimble.platform.server.plugins.messenger.socketio;

import java.util.Iterator;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.messaging.Messenger;
import com.bluenimble.platform.messenger.impls.socketio.SocketIoMessenger;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

public class SocketIoMessengerPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	public interface Spec {
		String Uri 			= "uri";
		String AuthField	= "authField";
		String ForceNew		= "forceNew";
		String Multiplex	= "multiplex";
		String Secure		= "secure";
		String Timeout		= "timeout";
		String RememberUpgrade
							= "rememberUpgrade";
		interface Reconnect	{
			String Enabled	= "enabled";
			String Attempts	= "attempts";
			String Delay	= "delay";
			String MaxDelay	= "maxDelay";
		}
	}
	
	private String 		feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Messenger.class.getAnnotation (Feature.class);
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
				return Messenger.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return ((RecyclableMessenger)(space.getRecyclable (createKey (name)))).messenger ();
			}
			@Override
			public String provider () {
				return SocketIoMessengerPlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return SocketIoMessengerPlugin.this;
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
		// create sessions
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
		
		String sessionKey = createKey (name);
		if (space.containsRecyclable (sessionKey)) {
			return;
		}
		
		JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
	
		if (overwrite) {
			removeClient (space, name);
		}
		
		space.addRecyclable (sessionKey, new RecyclableMessenger (new SocketIoMessenger (tracer, spec)));
	
		feature.set (ApiSpace.Spec.Installed, true);
	}
	
	private void removeClient (ApiSpace space, String featureName) {
		String key = createKey (featureName);
		Recyclable recyclable = space.getRecyclable (createKey (featureName));
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

	class RecyclableMessenger implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private SocketIoMessenger messenger;
		
		public RecyclableMessenger (SocketIoMessenger messenger) {
			this.messenger = messenger;
		}
		
		@Override
		public void finish (boolean withError) {
			// nothing
		}

		@Override
		public void recycle () {
			messenger.clear ();
		}

		public SocketIoMessenger messenger () {
			return messenger;
		}
		
	}

}

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

import java.net.URISyntaxException;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.messaging.Messenger;
import com.bluenimble.platform.messenger.impls.socketio.SocketIoMessenger;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ServerFeature;

public class SocketIoMessengerPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	interface Spec {
		String Uri 			= "uri";
		String AuthField	= "authField";
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
				JsonObject oSpec = (JsonObject)Json.find (space.getFeatures (), feature, name, Spec.class.getSimpleName ().toLowerCase ());
				if (oSpec == null) {
					throw new RuntimeException ("feature " + feature + " / " + name + "/ spec not found");
				}
				try {
					return new SocketIoMessenger (tracer, Json.getString (oSpec, Spec.Uri), Json.getString (oSpec, Spec.AuthField));
				} catch (URISyntaxException ex) {
					throw new RuntimeException (ex.getMessage (), ex);
				}
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
	
}

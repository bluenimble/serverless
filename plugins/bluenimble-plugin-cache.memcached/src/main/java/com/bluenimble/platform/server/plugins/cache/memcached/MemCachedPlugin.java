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
package com.bluenimble.platform.server.plugins.cache.memcached;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.cache.Cache;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

import com.bluenimble.platform.cache.impls.memcached.MemCachedCache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;

public class MemCachedPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	interface Spec {
		String Cluster 	= "cluster";
		
		String Auth 		= "auth";
			String User 	= "user";
			String Password = "password";
	}
	
	private String 		feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Cache.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = 3585173809402444745L;
			@Override
			public Class<?> type () {
				return Cache.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return new MemCachedCache (((RecyclableCacheClient)(space.getRecyclable (createKey (name)))).client ());
			}
			@Override
			public String provider () {
				return MemCachedPlugin.this.getName ();
			}
			@Override
			public Plugin implementor () {
				return MemCachedPlugin.this;
			}
		});
	}

	@Override
	public void onEvent (Event event, Object target) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		switch (event) {
			case Create:
				createClients ((ApiSpace)target);
				break;
			case AddFeature:
				// if it's Messenger and provider is 'smtp' create createSession
				createClients ((ApiSpace)target);
				break;
			case DeleteFeature:
				// if it's Messenger and provider is 'smtp' shutdown session
				dropClients ((ApiSpace)target);
				break;
			default:
				break;
		}
	}
	
	private void createClients (ApiSpace space) throws PluginRegistryException {
		// create sessions
		JsonObject msgFeature = Json.getObject (space.getFeatures (), feature);
		if (msgFeature == null || msgFeature.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = msgFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			
			JsonObject feature = Json.getObject (msgFeature, key);
			
			if (!this.getName ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
				continue;
			}
			
			String sessionKey = createKey (key);
			if (space.containsRecyclable (sessionKey)) {
				continue;
			}
			
			JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
		
			String [] nodes = Lang.split (Json.getString (spec, Spec.Cluster), Lang.COMMA);
			if (nodes == null) {
				continue;
			}
			
			final JsonObject oAuth = Json.getObject (spec, Spec.Auth);
			if (oAuth == null || oAuth.isEmpty ()) {
				continue;
			}
			
			final String user 		= Json.getString (oAuth, Spec.User);
			final String password 	= Json.getString (oAuth, Spec.Password);
			
			AuthDescriptor ad = new AuthDescriptor (new String [] { "PLAIN" }, new PlainCallbackHandler (user, password));

			try {
				MemcachedClient client = new MemcachedClient (
					new ConnectionFactoryBuilder ()
						.setProtocol (ConnectionFactoryBuilder.Protocol.BINARY)
						.setAuthDescriptor (ad).build (),
					AddrUtil.getAddresses (Arrays.asList (nodes))
				);
				
				space.addRecyclable (sessionKey, new RecyclableCacheClient (client));
				
			} catch (IOException e) {
				throw new PluginRegistryException (e.getMessage (), e);
			}
			
		}
		
	}
	
	private void dropClients (ApiSpace space) {
		
		JsonObject cahceFeature = Json.getObject (space.getFeatures (), feature);
		
		Set<String> recyclables = space.getRecyclables ();
		for (String r : recyclables) {
			if (!r.startsWith (feature + Lang.DOT)) {
				continue;
			}
			String name = r.substring ((feature + Lang.DOT).length ());
			if (cahceFeature == null || cahceFeature.containsKey (name)) {
				// it's deleted
				RecyclableCacheClient rm = (RecyclableCacheClient)space.getRecyclable (r);
				// remove from recyclables
				space.removeRecyclable (r);
				// recycle
				rm.recycle ();
			}
		}
	}
	
	private String createKey (String name) {
		return feature + Lang.DOT + name;
	}
	
	class RecyclableCacheClient implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private MemcachedClient client;
		
		public RecyclableCacheClient (MemcachedClient client) {
			this.client = client;
		}
		
		@Override
		public void recycle () {
			client.shutdown (2, TimeUnit.SECONDS);
		}

		public MemcachedClient client () {
			return (MemcachedClient) get ();
		}

		@Override
		public Object get () {
			return client;
		}

		@Override
		public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
			
		}
		
	}
	
}

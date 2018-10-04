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
import java.util.concurrent.TimeUnit;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.cache.Cache;
import com.bluenimble.platform.cache.impls.memcached.MemCachedCache;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

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
			public String id () {
				return null;
			}
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
				return MemCachedPlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return MemCachedPlugin.this;
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
				removeClient ((ApiSpace)target, (String)args [0]);
				break;
			default:
				break;
		}
	}
	
	private void createClients (ApiSpace space) throws PluginRegistryException {
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
	
	private void createClient (ApiSpace space, JsonObject allFeatures, String name, boolean overwrite) throws PluginRegistryException {
		
		JsonObject feature = Json.getObject (allFeatures, name);
		
		if (Json.isNullOrEmpty (feature)) {
			return;
		}
		
		if (!this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
			return;
		}
		
		String sessionKey = createKey (name);
		if (space.containsRecyclable (sessionKey)) {
			return;
		}
		
		JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
	
		String [] nodes = Lang.split (Json.getString (spec, Spec.Cluster), Lang.COMMA);
		if (nodes == null) {
			return;
		}
		
		final JsonObject oAuth = Json.getObject (spec, Spec.Auth);
		if (oAuth == null || oAuth.isEmpty ()) {
			return;
		}
		
		final String user 		= Json.getString (oAuth, Spec.User);
		final String password 	= Json.getString (oAuth, Spec.Password);
		
		AuthDescriptor ad = new AuthDescriptor (new String [] { "PLAIN" }, new PlainCallbackHandler (user, password));

		MemcachedClient client = null;
		try {
			client = new MemcachedClient (
				new ConnectionFactoryBuilder ()
					.setProtocol (ConnectionFactoryBuilder.Protocol.BINARY)
					.setAuthDescriptor (ad).build (),
				AddrUtil.getAddresses (Arrays.asList (nodes))
			);
			
		} catch (IOException e) {
			throw new PluginRegistryException (e.getMessage (), e);
		}
		
		if (overwrite) {
			removeClient (space, name);
		}
		
		space.addRecyclable (sessionKey, new RecyclableCacheClient (client));
		
		feature.set (ApiSpace.Spec.Installed, true);
		
	}
	
	private void removeClient (ApiSpace space, String featureName) {
		String key = createKey (featureName);
		Recyclable recyclable = space.getRecyclable (key);
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
	
	class RecyclableCacheClient implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private MemcachedClient client;
		
		public RecyclableCacheClient (MemcachedClient client) {
			this.client = client;
		}
		
		@Override
		public void finish (boolean withError) {
		}

		@Override
		public void recycle () {
			client.shutdown (2, TimeUnit.SECONDS);
		}

		public MemcachedClient client () {
			return client;
		}
		
	}
	
}

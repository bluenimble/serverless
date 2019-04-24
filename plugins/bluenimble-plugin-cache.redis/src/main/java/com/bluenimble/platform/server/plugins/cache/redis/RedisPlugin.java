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
package com.bluenimble.platform.server.plugins.cache.redis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.cache.Cache;
import com.bluenimble.platform.cache.impls.redis.RedisCache;
import com.bluenimble.platform.cache.impls.redis.RedisClusterCache;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	interface Spec {
		String Cluster 	= "cluster";
			String Host 	= "host";
			String Port 	= "port";
			
		String Pool		= "pool";
			String MaxIdle
						= "maxIdle";
			String MaxTotal
						= "maxTotal";
			String TestOnBorrow
						= "testOnBorrow";
			String TestOnReturn
						= "testOnReturn";
			String ConnectTimeout
						= "connectTimeout";
		
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
				RecyclableCacheClient r = (RecyclableCacheClient)space.getRecyclable (createKey (name));
				if (r.isCluster ()) {
					return new RedisClusterCache (r.cluster ());
				} else {
					return new RedisCache (r.node ());
				}
			}
			@Override
			public String provider () {
				return RedisPlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return RedisPlugin.this;
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

		JsonArray nodes = Json.getArray (spec, Spec.Cluster);
		if (nodes == null) {
			return;
		}
		
		String password = (String)Json.find (spec, Spec.Auth, Spec.Password);
		
		boolean cluster = false;
		
		Object client;
		
		if (nodes.count () == 1) {
			JsonObject oNode = (JsonObject)nodes.get (0);
			
			JsonObject oPool = Json.getObject (spec, Spec.Pool);
			
			JedisPoolConfig config = new JedisPoolConfig ();
			//Maximum idle connections, which are evaluated by the application. Do not set it to a value greater than the maximum connections of an ApsaraDB for Redis instance.
			config.setMaxIdle (Json.getInteger (oPool, Spec.MaxIdle, 30));
			//Maximum connections, which are evaluated by the application. Do not set it to a value greater than the maximum connections of an ApsaraDB for Redis instance.
			config.setMaxTotal (Json.getInteger (oPool, Spec.MaxTotal, 50));
			config.setTestOnBorrow (Json.getBoolean (oPool, Spec.TestOnBorrow, false));
			config.setTestOnReturn (Json.getBoolean (oPool, Spec.TestOnReturn, false));
			
			client = new JedisPool (
				config, 
				Json.getString (oNode, Spec.Host), 
				Json.getInteger (oNode, Spec.Port, 6379), Json.getInteger (oPool, Spec.ConnectTimeout, 3000) , password);
			
		} else {
			Set<HostAndPort> lNodes = new HashSet<HostAndPort>();
			
			for (int i = 0; i < nodes.count (); i++) {
				JsonObject oNode = (JsonObject)nodes.get (i);
				lNodes.add (new HostAndPort (Json.getString (oNode, Spec.Host), Json.getInteger (oNode, Spec.Port, 6379)));
			}
			
			client = new JedisCluster (lNodes);

			cluster = true;
		}
		
		if (overwrite) {
			removeClient (space, name);
		}
		
		space.addRecyclable (sessionKey, new RecyclableCacheClient (client, cluster));
		
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

		private Object client;
		private boolean cluster;
		
		public RecyclableCacheClient (Object client, boolean cluster) {
			this.client = client;
			this.cluster = cluster;
		}
		
		@Override
		public void finish (boolean withError) {
		}

		@Override
		public void recycle () {
			if (cluster) {
				((JedisCluster)client).close ();
			} else {
				((JedisPool)client).close ();
			}
		}

		public JedisCluster cluster () {
			return (JedisCluster)client;
		}
		
		public Jedis node () {
			return ((JedisPool)client).getResource ();
		}
		
		public boolean isCluster () {
			return cluster;
		}
		
	}
	
}

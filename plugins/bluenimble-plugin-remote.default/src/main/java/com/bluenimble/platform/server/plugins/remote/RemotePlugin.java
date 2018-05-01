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

import java.util.Iterator;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.pooling.PoolConfig;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.impls.binary.BinaryRemote;
import com.bluenimble.platform.remote.impls.http.HttpRemote;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;
import com.bluenimble.platform.tools.binary.BinaryClient;
import com.bluenimble.platform.tools.binary.BinaryClientFactory;
import com.bluenimble.platform.tools.binary.impls.netty.NettyBinaryClientFactory;

public class RemotePlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;

	enum Protocol {
		http,
		binary
	}
	
	private String 	feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Remote.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}

		feature = aFeature.name ();

		PackageClassLoader pcl = (PackageClassLoader)RemotePlugin.class.getClassLoader ();
		
		pcl.registerObject (Protocol.http.name (), new HttpRemote ());
		
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = -9012279234275100528L;
			
			@Override
			public Class<?> type () {
				return Remote.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				JsonObject spec = (JsonObject)Json.find (space.getFeatures (), feature, name, ApiSpace.Features.Spec);
				String protocol = Json.getString (spec, Remote.Spec.Protocol);

				Remote remote = null;
				
				if (Protocol.binary.name ().equalsIgnoreCase (protocol)) {
					remote = new BinaryRemote (
						((RecyclableBinaryClientFactory)space.getRecyclable (createKey (name, space))).create ()
					);
				} else {
					remote = new HttpRemote (space, name, spec);
				}
				return remote;
			}
			@Override
			public String provider () {
				return RemotePlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return RemotePlugin.this;
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
				createFactories (space);
				break;
			case AddFeature:
				createFactories (space);
				break;
			case DeleteFeature:
				deleteFactories (space);
				break;
			default:
				break;
		}
	}
	
	private void createFactories (ApiSpace space) {
		JsonObject remoteFeatures = Json.getObject (space.getFeatures (), feature);
		if (remoteFeatures == null || remoteFeatures.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = remoteFeatures.keys ();
		while (keys.hasNext ()) {
			String name = keys.next ();
			JsonObject featureSpec = Json.getObject (remoteFeatures, name);
			createFactory (space, name, featureSpec);
		}
	}
	
	private void createFactory (ApiSpace space, String name, JsonObject featureSpec) {
		if (!this.getNamespace ().equalsIgnoreCase (Json.getString (featureSpec, ApiSpace.Features.Provider))) {
			return;
		}
		
		JsonObject spec = Json.getObject (featureSpec, ApiSpace.Features.Spec);

		String sProtocol = Json.getString (spec, Remote.Spec.Protocol);
		
		// only if binary. Http remote is accessible directly from the plugin feature
		if (!Protocol.binary.name ().equalsIgnoreCase (sProtocol)) {
			return;
		}
		
		String factoryKey = createKey (name, space);
		
		Recyclable recyclable = space.getRecyclable (factoryKey);
		if (recyclable != null) {
			return;
		}
		
		String 	host = Json.getString (spec, Remote.Spec.Host);
		int 	port = Json.getInteger (spec, Remote.Spec.Port, 0);
		
		if (Lang.isNullOrEmpty (host) || port == 0) {
			return;
		}
		
		if (spec == null) {
			spec = JsonObject.Blank;
		}
				
		space.addRecyclable (
			factoryKey, 
			new RecyclableBinaryClientFactory (
				new NettyBinaryClientFactory (host, port, new PoolConfig (Json.getObject (spec, Remote.Spec.Pool)))
			)
		);
	}
	
	private void deleteFactories (ApiSpace space) {
		JsonObject remoteFeatures = Json.getObject (space.getFeatures (), feature);
		if (remoteFeatures == null || remoteFeatures.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = remoteFeatures.keys ();
		while (keys.hasNext ()) {
			String name = keys.next ();
			
			String factoryKey = createKey (name, space);
			Recyclable recyclable = space.getRecyclable (factoryKey);
			if (recyclable == null) {
				continue;
			}
			recyclable.recycle ();
			space.removeRecyclable (factoryKey);
			
		}
		
	}
	
	private String createKey (String name, ApiSpace space) {
		return feature + Lang.DOT + space.getNamespace () + Lang.DOT + name;
	}
	
	class RecyclableBinaryClientFactory implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private BinaryClientFactory factory;
		
		public RecyclableBinaryClientFactory (BinaryClientFactory factory) {
			this.factory = factory;
		}
		
		@Override
		public void recycle () {
			factory.shutdown ();
		}

		public BinaryClient create () {
			return factory.create ();
		}

		@Override
		public Object get () {
			return factory;
		}

		@Override
		public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
			
		}
		
	}
	
}

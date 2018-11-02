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
package com.bluenimble.platform.server.plugins.indexer.elasticsearch;

import java.util.Iterator;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.indexer.Indexer;
import com.bluenimble.platform.indexer.impls.ElasticSearchIndexer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ServerFeature;
import com.bluenimble.platform.server.ApiServer.Event;

public class ElasticSearchPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	interface Spec {
		String Remote = "remote";
		String Index = "index";
	}
	
	private String feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Indexer.class.getAnnotation (Feature.class);
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
				return Indexer.class;
			}
			
			@Override
			public Object get (ApiSpace space, String name) {
				String remoteFeature = (String)Json.find (space.getFeatures (), feature, name, ApiSpace.Features.Spec, Spec.Remote);
				String index 		 = (String)Json.find (space.getFeatures (), feature, name, ApiSpace.Features.Spec, Spec.Index);
				tracer.log (Level.Info, "Indexer Feature Remote " + remoteFeature);
				Remote remote = null;
				if (!Lang.isNullOrEmpty (remoteFeature)) {
					remote = space.feature (Remote.class, remoteFeature, ApiContext.Instance);
				}
				
				return new ElasticSearchIndexer (remote, index, tracer);
			}
			
			@Override
			public String provider () {
				return ElasticSearchPlugin.this.getNamespace ();
			}
			
			@Override
			public Plugin implementor () {
				return ElasticSearchPlugin.this;
			}
		});
	}
	
	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		JsonObject allFeatures = Json.getObject (space.getFeatures (), feature);
		
		if (Json.isNullOrEmpty (allFeatures)) {
			return;
		}
		
		switch (event) {
			case Create:
				Iterator<String> keys = allFeatures.keys ();
				while (keys.hasNext ()) {
					String name = keys.next ();
					JsonObject feature = Json.getObject (allFeatures, name);
					if (!this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
						continue;
					}
					feature.set (ApiSpace.Spec.Installed, true);
				}
				break;
			case AddFeature:
				JsonObject feature = Json.getObject (allFeatures, (String)args [0]);
				if (this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
					feature.set (ApiSpace.Spec.Installed, true);
				}
				break;
			default:
				break;
		}
	}
	
}
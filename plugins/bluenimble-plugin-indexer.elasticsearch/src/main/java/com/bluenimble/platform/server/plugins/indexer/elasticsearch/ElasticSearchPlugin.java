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

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.indexer.Indexer;
import com.bluenimble.platform.indexer.impls.ElasticSearchIndexer;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ServerFeature;

public class ElasticSearchPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	interface Spec {
		String Remote = "remote";
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
				tracer.log (Level.Info, "Indexer Feature Remote " + remoteFeature);
				Remote remote = null;
				if (!Lang.isNullOrEmpty (remoteFeature)) {
					remote = space.feature (Remote.class, remoteFeature, ApiContext.Instance);
				}
				
				return new ElasticSearchIndexer (remote, tracer);
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

	/*
	public JsonObject getRemote () {
		return remote;
	}

	public void setRemote (JsonObject remote) {
		this.remote = remote;
	}

	private String buildUrl (ApiSpace space, String name) {
		JsonObject spec = getSpec (space, name);
		if (spec == null) {
			return null;
		}
		
		String host 	= Json.getString (spec, SpaceSpec.Host);
		
		String index 	= Json.getString (spec, SpaceSpec.Index);
		if (Lang.isNullOrEmpty (host) || Lang.isNullOrEmpty (index)) {
			return null;
		}
		
		if (!host.endsWith (Lang.SLASH)) {
			host += Lang.SLASH;
		}
		
		return host + index + Lang.SLASH;
	}
	
	private String buildAuthToken (ApiSpace space, String name) {

		JsonObject spec = getSpec (space, name);
		if (spec == null) {
			return null;
		}
		
		JsonObject auth = Json.getObject (spec, SpaceSpec.Auth);
		if (Json.isNullOrEmpty (auth)) {
			return null;
		}
		
		String userName = Json.getString (auth, SpaceSpec.User);
		String userPwd 	= Json.getString (auth, SpaceSpec.Password);
		if (Lang.isNullOrEmpty (userName) || Lang.isNullOrEmpty (userPwd)) {
			return null;
		}
		
		String combination = userName + Lang.COLON + userPwd;
		return "Basic " + Base64.getEncoder ().encodeToString (combination.getBytes ());
	}
	
	private JsonObject getSpec (ApiSpace space, String name) {
		JsonObject indexerFeatures = Json.getObject (space.getFeatures (), feature);
		if (indexerFeatures == null || indexerFeatures.isEmpty ()) {
			return null;
		}
		
		JsonObject indexerFeature = Json.getObject (indexerFeatures, name);
		if (indexerFeature == null || indexerFeature.isEmpty ()) {
			return null;
		}
		
		if (!this.getNamespace ().equalsIgnoreCase (Json.getString (indexerFeature, ApiSpace.Features.Provider))) {
			return null;
		}
		
		return Json.getObject (indexerFeature, ApiSpace.Features.Spec);
	}
	*/
}
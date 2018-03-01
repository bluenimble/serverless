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

import java.util.Base64;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.indexer.Indexer;
import com.bluenimble.platform.indexer.impls.ElasticSearchIndexer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

public class ElasticSearchPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;

	private static final String 	Provider 		= "bnb-indexer";
	
	private interface Spec {
		String 	RemotePlugin 	= "plugin";
		String 	RemoteObject 	= "object";
	}
	
	private interface SpaceSpec {
		String 	Host 		= "host";
		String 	Index 		= "index";
		
		String 	Auth 		= "auth";
			String 	User 		= "user";
			String 	Password 	= "password";
	}
	
	private String feature;
	
	private JsonObject remote;
	
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
			public Class<?> type () {
				return Indexer.class;
			}
			
			@Override
			public Object get (ApiSpace space, String name) {
				
				String remotePluginName = Json.getString (remote, Spec.RemotePlugin);
				String remoteObjectId 	= Json.getString (remote, Spec.RemoteObject);
				
				if (Lang.isNullOrEmpty (remotePluginName) || Lang.isNullOrEmpty (remoteObjectId)) {
					return null;
				}
				
				Plugin pRemote = server.getPluginsRegistry ().lockup (Json.getString (remote, Spec.RemotePlugin));
				if (pRemote == null) {
					return null;
				}
				
				PackageClassLoader pcl = (PackageClassLoader)pRemote.getClass ().getClassLoader ();
				
				Remote iRemote = (Remote)pcl.lookupObject (Json.getString (remote, Spec.RemoteObject));
				if (iRemote == null) {
					return null;
				}
				
				// build basic auth and url 
				
				String token = buildAuthToken (space, name);
				
				if (Lang.isNullOrEmpty (token)) {
					return null;
				}
				
				String url = buildUrl (space, name);

				if (Lang.isNullOrEmpty (url)) {
					return null;
				}
				
				return new ElasticSearchIndexer (iRemote, url, token, tracer ());
			}
			
			@Override
			public String provider () {
				return Provider;
			}
			
			@Override
			public Plugin implementor () {
				return ElasticSearchPlugin.this;
			}
		});
	}

	@Override
	public void onEvent (Event event, Object target) throws PluginRegistryException {
		
	}
	
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
		
		if (!Provider.equalsIgnoreCase (Json.getString (indexerFeature, ApiSpace.Features.Provider))) {
			return null;
		}
		
		return Json.getObject (indexerFeature, ApiSpace.Features.Spec);
	}
}
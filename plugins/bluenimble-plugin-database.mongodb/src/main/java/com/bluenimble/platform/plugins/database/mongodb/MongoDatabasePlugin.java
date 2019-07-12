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
package com.bluenimble.platform.plugins.database.mongodb;

import java.util.Iterator;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.database.mongodb.impls.MongoDatabaseImpl;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;

public class MongoDatabasePlugin extends AbstractPlugin {

	private static final long serialVersionUID 		= -6219529665471192558L;
	
	private CodecRegistry codecRegistry;
	
	private static final String DefaultDriver = "mongodb+srv";
	
	interface Spec {
		String Host 	= "host";
		String Database = "database";
		String Driver 	= "driver";
		
		String SSL 		= "ssl";
		String MaxConnections
						= "maxConnections";
		String ThreadsAllowedToBlock
						= "threadsAllowedToBlock";
		String MaxWaitTime
						= "maxWaitTime";
		String ConnectTimeout 	
						= "connectTimeout"; 
		String SocketTimeout
						= "socketTimeout";
		
		String RetryWrites
						= "retryWrites";
		
		String Auth 	= "auth";
			String User 	= "user";
			String Password = "password";
			
		String AllowProprietaryAccess
						= "allowProprietaryAccess";
		
		String CaseSensitive
						= "caseSensitive";
	}
	
	private String				feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Database.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		// add features
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = 2626039344401539390L;
			@Override
			public String id () {
				return null;
			}
			@Override
			public Class<?> type () {
				return Database.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				Object oAllowProprietaryAccess = 
					Json.find (space.getFeatures (), feature, name, ApiSpace.Features.Spec, Spec.AllowProprietaryAccess);
				boolean allowProprietaryAccess = 
						oAllowProprietaryAccess == null || String.valueOf (oAllowProprietaryAccess).equalsIgnoreCase (Lang.TRUE);
				
				Object oCaseSensitive = 
					Json.find (space.getFeatures (), feature, name, ApiSpace.Features.Spec, Spec.CaseSensitive);
				boolean caseSensitive = 
						oCaseSensitive == null || String.valueOf (oCaseSensitive).equalsIgnoreCase (Lang.TRUE);

				RecyclableClient rClient = (RecyclableClient)space.getRecyclable (createKey (name));
				
				return new MongoDatabaseImpl (rClient.client, rClient.database, tracer (), caseSensitive, allowProprietaryAccess);
				
			}
			@Override
			public Plugin implementor () {
				return MongoDatabasePlugin.this;
			}
			@Override
			public String provider () {
				return MongoDatabasePlugin.this.getNamespace ();
			}
		});
		
		codecRegistry = CodecRegistries.fromRegistries (
			MongoClient.getDefaultCodecRegistry (), 
			CodecRegistries.fromProviders (PojoCodecProvider.builder ().automatic (true).build ())
		);

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
		// create factories
		JsonObject allFeatures = Json.getObject (space.getFeatures (), feature);
		if (Json.isNullOrEmpty (allFeatures)) {
			return;
		}
		
		Iterator<String> keys = allFeatures.keys ();
		while (keys.hasNext ()) {
			createClient (space, allFeatures, keys.next (), false);
		}
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
	
	private MongoClient createClient (ApiSpace space, JsonObject allFeatures, String name, boolean overwrite) {
		
		JsonObject feature = Json.getObject (allFeatures, name);
		
		if (!MongoDatabasePlugin.this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
			return null;
		}
		
		JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
		
		if (spec == null) {
			return null;
		}
		
		String factoryKey = createKey  (name);
		
		if (space.containsRecyclable (factoryKey)) {
			return null;
		}
		
		String database = Json.getString (spec, Spec.Database);
		if (Lang.isNullOrEmpty (database)) {
			return null;
		}
		
		String host = Json.getString (spec, Spec.Host);
		if (Lang.isNullOrEmpty (host)) {
			return null;
		}
		
		String driver = Json.getString (spec, Spec.Driver, DefaultDriver);

		MongoClientOptions.Builder optionsBuilder = 
				MongoClientOptions.builder ()
					.cursorFinalizerEnabled (false)
					.retryWrites (Json.getBoolean (spec, Spec.RetryWrites, true))
					.codecRegistry (codecRegistry)
					.sslEnabled (Json.getBoolean (spec, Spec.SSL, false))
					.connectionsPerHost (Json.getInteger (spec, Spec.MaxConnections, 100))
					.threadsAllowedToBlockForConnectionMultiplier (Json.getInteger (spec, Spec.ThreadsAllowedToBlock, 5))
					.maxWaitTime (Json.getInteger (spec, Spec.MaxWaitTime, 1000))
					.connectTimeout (Json.getInteger (spec, Spec.ConnectTimeout, 10000))
					.socketTimeout (Json.getInteger (spec, Spec.SocketTimeout, 0));
		
		String credentials = null;
		
		JsonObject auth = Json.getObject (spec, Spec.Auth);
		if (!Json.isNullOrEmpty (auth)) {
			credentials = Json.getString (auth, Spec.User) + Lang.COLON + Json.getString (auth, Spec.Password);
		}
		
		MongoClientURI uri = new MongoClientURI (
			driver + "://" + (credentials == null ? Lang.BLANK : credentials + Lang.AT) + host,
			optionsBuilder
		);
				
		MongoClient client = new MongoClient (uri);
		
		if (overwrite) {
			removeClient (space, name);
		}
		
		space.addRecyclable (factoryKey, new RecyclableClient (client, database));
		
		feature.set (ApiSpace.Spec.Installed, true);
		
		return client;
		
	}
	
	private String createKey (String name) {
		return feature + Lang.DOT + getNamespace () + Lang.DOT + name;
	}
	
	class RecyclableClient implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private MongoClient client;
		private String 		database;
		
		public RecyclableClient (MongoClient client, String database) {
			this.client 		= client;
			this.database 		= database;
		}
		
		@Override
		public void finish (boolean withError) {
		}

		@Override
		public void recycle () {
			try {
				client.close ();
			} catch (Exception ex) {
				// Ignore
			}
		}
		
	}
}

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
import java.util.Set;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.tracing.Tracer;
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
import com.mongodb.client.MongoDatabase;

/**
 * 
 * TODO:
 * 	- Options
 * 	- Pool size 
 * 
 **/
public class MongoDatabasePlugin extends AbstractPlugin {

	private static final long serialVersionUID 		= -6219529665471192558L;
	
	private static final String Protocol = "mongodb://";
	
	private CodecRegistry codecRegistry;
	
	interface Spec {
		String Cluster 	= "cluster";
		String Database = "database";
		
		String User 	= "user";
		String Password = "password";
	}
	
	private String				feature;
	
	//private int 				weight;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		//weight = server.weight ();
		
		Feature aFeature = Database.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		// add features
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = 2626039344401539390L;
			@Override
			public Class<?> type () {
				return Database.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return new MongoDatabaseImpl (MongoDatabasePlugin.this.acquire (space, name), tracer ());
				
			}
			@Override
			public Plugin implementor () {
				return MongoDatabasePlugin.this;
			}
			@Override
			public String provider () {
				return MongoDatabasePlugin.this.getName ();
			}
		});
		
		codecRegistry = CodecRegistries.fromRegistries (
			MongoClient.getDefaultCodecRegistry (), 
			CodecRegistries.fromProviders (PojoCodecProvider.builder ().automatic (true).build ())
		);

	}
	
	@Override
	public void onEvent (Event event, Object target) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		tracer ().log (Tracer.Level.Info, "onEvent {0}, target {1}", event, target.getClass ().getSimpleName ());
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Create:
				createClients (space);
				break;
			case AddFeature:
				// if it's database and provider is 'mongodb' create clients
				createClients (space);
				break;
			case DeleteFeature:
				// if it's database and provider is 'mongodb' stop factory
				dropClients (space);
				break;
			default:
				break;
		}
	}
	
	private void createClients (ApiSpace space) {
		
		// create factories
		JsonObject dbFeature = Json.getObject (space.getFeatures (), feature);
		if (dbFeature == null || dbFeature.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = dbFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject source = Json.getObject (dbFeature, key);
			
			if (!MongoDatabasePlugin.this.getName ().equalsIgnoreCase (Json.getString (source, ApiSpace.Features.Provider))) {
				continue;
			}
			
			JsonObject spec = Json.getObject (source, ApiSpace.Features.Spec);
			
			if (spec == null) {
				continue;
			}
			
			createClient (key, space, spec);
		}
	}
	
	private void dropClients (ApiSpace space) {
		
		JsonObject dbFeature = Json.getObject (space.getFeatures (), feature);
		
		Set<String> recyclables = space.getRecyclables ();
		for (String r : recyclables) {
			if (!r.startsWith (feature + Lang.DOT)) {
				continue;
			}
			String name = r.substring ((feature + Lang.DOT).length ());
			if (dbFeature == null || dbFeature.containsKey (name)) {
				// it's deleted
				RecyclableClient rf = (RecyclableClient)space.getRecyclable (r);
				// remove from recyclables
				space.removeRecyclable (r);
				// recycle
				rf.recycle ();
			}
		}
		
	}
	
	private MongoClient createClient (String name, ApiSpace space, JsonObject spec) {
		
		String factoryKey = createFactoryKey  (name, space);
		
		if (space.containsRecyclable (factoryKey)) {
			return null;
		}
		
		MongoClientURI uri = new MongoClientURI (
			createUrl (spec),
			MongoClientOptions.builder ().cursorFinalizerEnabled (false).codecRegistry (codecRegistry)
			// set other options
			// pool
		);
		
		MongoClient client = new MongoClient (uri);
		
		space.addRecyclable (factoryKey, new RecyclableClient (client, Json.getString (spec, Spec.Database)));
		
		return client;
		
	}
	
	private String createFactoryKey (String name, ApiSpace space) {
		return feature + Lang.DOT + space.getNamespace () + Lang.DOT + name;
	}

	private String createUrl (JsonObject database) {
		return Protocol + Json.getString (database, Spec.Cluster) + Lang.SLASH;
	}
	
	public MongoDatabase acquire (ApiSpace space, String name) {
		return ((RecyclableClient)space.getRecyclable (createFactoryKey (name, space))).database ();
	}
	
	class RecyclableClient implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private MongoClient client;
		private String 		database;
		
		public RecyclableClient (MongoClient client, String database) {
			this.client 	= client;
			this.database 	= database;
		}
		
		@Override
		public void recycle () {
			try {
				client.close ();
			} catch (Exception ex) {
				// Ignore
			}
		}

		public MongoDatabase database () {
			return ((MongoClient)get ()).getDatabase (database);
		}

		@Override
		public Object get () {
			return client;
		}

		@Override
		public void set (ApiSpace arg0, ClassLoader arg1, Object... arg2) {
			
		}
		
	}
}

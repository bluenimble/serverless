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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

public class MongoDatabasePlugin extends AbstractPlugin {

	private static final long serialVersionUID 		= -6219529665471192558L;
	
	private CodecRegistry codecRegistry;
	
	interface Spec {
		String Cluster 	= "cluster";
		String Database = "database";
		
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
		
		String Auth 	= "auth";
			String Type 	= "type";
			String User 	= "user";
			String Password = "password";
			
		String AllowProprietaryAccess
						= "allowProprietaryAccess";
	}
	
	enum AuthType {
		SHA,
		CR,
		X509,
		KERBEROS,
		LDAP
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
				return new MongoDatabaseImpl (MongoDatabasePlugin.this.acquire (space, name), tracer (), allowProprietaryAccess);
				
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
		
		MongoCredential creds = createCredentials (database, Json.getObject (spec, Spec.Auth));
		
		String cluster = Json.getString (spec, Spec.Cluster);
		if (cluster == null) {
			cluster = Lang.BLANK;
		}
		
		String [] sNodes = Lang.split (Json.getString (spec, Spec.Cluster), Lang.COMMA, true);
		
		List<ServerAddress> nodes = new ArrayList<ServerAddress> (sNodes.length);
				
		for (int i = 0; i < sNodes.length; i++) {
			nodes.add (address (sNodes [i]));
		}
		
		MongoClientOptions options = 
				MongoClientOptions.builder ()
					.cursorFinalizerEnabled (false)
					.codecRegistry (codecRegistry)
					.sslEnabled (Json.getBoolean (spec, Spec.SSL, false))
					.connectionsPerHost (Json.getInteger (spec, Spec.MaxConnections, 100))
					.threadsAllowedToBlockForConnectionMultiplier (Json.getInteger (spec, Spec.ThreadsAllowedToBlock, 5))
					.maxWaitTime (Json.getInteger (spec, Spec.MaxWaitTime, 1000))
					.connectTimeout (Json.getInteger (spec, Spec.ConnectTimeout, 10000))
					.socketTimeout (Json.getInteger (spec, Spec.SocketTimeout, 0))
					.build ();
		// apply other options such as pool
		
		MongoClient client = null;
		if (creds == null) {
			client = new MongoClient (nodes, options);
		} else {
			client = new MongoClient (nodes, creds, options);
		}
		
		if (overwrite) {
			removeClient (space, name);
		}
		
		space.addRecyclable (factoryKey, new RecyclableClient (client, database));
		
		feature.set (ApiSpace.Spec.Installed, true);
		
		return client;
		
	}
	
	private ServerAddress address (String hostAndPort) {
		String 	host = hostAndPort;
		int 	port = 27017;
		
		int indexOfColon = hostAndPort.lastIndexOf (Lang.COLON);
		if (indexOfColon > 0) {
			host = hostAndPort.substring (0, indexOfColon).trim ();
			try {
				port = Integer.valueOf (hostAndPort.substring (indexOfColon + 1).trim ());
			} catch (NumberFormatException ex) {
				// ignore, default to 27017 and host to hostAndPort
				host = hostAndPort;
			}
		}
		if (host.equals (Lang.BLANK)) {
			host = "localhost";
		}
		return new ServerAddress (host, port);
	}
	
	private MongoCredential createCredentials (String database, JsonObject auth) {
		
		String sAuthType = Json.getString (auth, Spec.Type);
		if (Lang.isNullOrEmpty (sAuthType)) {
			return null;
		}
		
		sAuthType = sAuthType.toUpperCase ();
		
		AuthType authType = null;
		
		try {
			authType = AuthType.valueOf (sAuthType);
		} catch (Exception ex) {
			// ignore, default to sha
		}
		
		if (authType == null) {
			authType = AuthType.SHA;
		}
 		
		String user 	= Json.getString (auth, Spec.User);
		String password = Json.getString (auth, Spec.Password);

		switch (authType) {
			case SHA:
				if (Lang.isNullOrEmpty (user) || Lang.isNullOrEmpty (password)) {
					return null;
				}
				return MongoCredential.createScramSha1Credential (user, database, password.toCharArray ());
			case CR:
				if (Lang.isNullOrEmpty (user) || Lang.isNullOrEmpty (password)) {
					return null;
				}
				return MongoCredential.createMongoCRCredential (user, database, password.toCharArray ());
			case X509:
				if (Lang.isNullOrEmpty (user)) {
					return null;
				}
				return MongoCredential.createMongoX509Credential (user);
			case KERBEROS:
				if (Lang.isNullOrEmpty (user)) {
					return null;
				}
				return MongoCredential.createGSSAPICredential (user);
			case LDAP:
				if (Lang.isNullOrEmpty (user) || Lang.isNullOrEmpty (password)) {
					return null;
				}
				return MongoCredential.createPlainCredential (user, "$external", password.toCharArray ());
			default:
				return null;
		}
		
	}
	
	private String createKey (String name) {
		return feature + Lang.DOT + getNamespace () + Lang.DOT + name;
	}
	
	public MongoDatabase acquire (ApiSpace space, String name) {
		return ((RecyclableClient)space.getRecyclable (createKey (name))).database ();
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

		public MongoDatabase database () {
			return client.getDatabase (database);
		}
		
	}
}

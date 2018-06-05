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
import java.util.Set;

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
				createClients (space);
				break;
			case DeleteFeature:
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
			
			if (!MongoDatabasePlugin.this.getNamespace ().equalsIgnoreCase (Json.getString (source, ApiSpace.Features.Provider))) {
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
		
		space.addRecyclable (factoryKey, new RecyclableClient (client, database));
		
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
	
	private String createFactoryKey (String name, ApiSpace space) {
		return feature + Lang.DOT + space.getNamespace () + Lang.DOT + name;
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

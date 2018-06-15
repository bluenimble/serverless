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
package com.bluenimble.platform.plugins.database.orientdb;

import java.util.Iterator;
import java.util.Set;

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
import com.bluenimble.platform.plugins.database.orientdb.impls.OrientDatabase;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class OrientDatabasePlugin extends AbstractPlugin {

	private static final long serialVersionUID 		= -6219529665471192558L;
	
	interface Spec {
		String Host 	= "host";
		String Port 	= "port";
		String Database = "database";
		
		String MaxPartitionSize = "maxPartitionSize";
		String MaxPoolSize 		= "maxPoolSize";
		
		String Auth 	= "auth";
			String User 	= "user";
			String Password = "password";
			
		String AllowProprietaryAccess
						= "allowProprietaryAccess";	
	}
	
	interface Protocol {
		String Remote	= "remote:";
		String Local 	= "plocal:";
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
				return new OrientDatabase (OrientDatabasePlugin.this.acquire (space, name), tracer (), allowProprietaryAccess);
				
			}
			@Override
			public Plugin implementor () {
				return OrientDatabasePlugin.this;
			}
			@Override
			public String provider () {
				return OrientDatabasePlugin.this.getNamespace ();
			}
		});
		
		if (Orient.instance () != null) {
			Orient.instance ().removeShutdownHook ();
		}
		
	}
	
	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Create:
				createPools (space);
				break;
			case AddFeature:
				createPools (space);
				break;
			case DeleteFeature:
				dropPools (space);
				break;
			default:
				break;
		}
	}
	
	private void createPools (ApiSpace space) {
		
		// create factories
		JsonObject allFeatures = Json.getObject (space.getFeatures (), feature);
		if (allFeatures == null || allFeatures.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = allFeatures.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject feature = Json.getObject (allFeatures, key);
			
			if (!this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
				continue;
			}
			
			JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
			
			if (spec == null) {
				continue;
			}
			
			OPartitionedDatabasePool pool = createPool (key, space, spec);
			if (pool != null) {
				feature.set (ApiSpace.Spec.Installed, true);
			}
		}
	}
	
	private void dropPools (ApiSpace space) {
		
		JsonObject dbFeature = Json.getObject (space.getFeatures (), feature);
		
		Set<String> recyclables = space.getRecyclables ();
		for (String r : recyclables) {
			if (!r.startsWith (feature + Lang.DOT)) {
				continue;
			}
			String name = r.substring ((feature + Lang.DOT).length ());
			if (dbFeature == null || dbFeature.containsKey (name)) {
				// it's deleted
				Recyclable recyclable = space.getRecyclable (r);
				if (!(recyclable instanceof RecyclablePool)) {
					continue;
				}
				// remove from recyclables
				space.removeRecyclable (r);
				// recycle
				recyclable.recycle ();
			}
		}
		
	}
	
	private OPartitionedDatabasePool createPool (String name, ApiSpace space, JsonObject spec) {
		
		String factoryKey = createFactoryKey  (name, space);
		
		if (space.containsRecyclable (factoryKey)) {
			return null;
		}
		
		JsonObject auth = Json.getObject (spec, Spec.Auth);
		if (Json.isNullOrEmpty (auth)) {
			return null;
		}
		
		OPartitionedDatabasePool pool = new OPartitionedDatabasePool (
			createUrl (spec), 
			Json.getString (auth, Spec.User), 
			Json.getString (auth, Spec.Password),
			Json.getInteger (spec, Spec.MaxPartitionSize, 10), 
			Json.getInteger (spec, Spec.MaxPoolSize, 10)
		);
		
		space.addRecyclable (factoryKey, new RecyclablePool (pool));
		
		return pool;
		
	}
	
	private String createFactoryKey (String name, ApiSpace space) {
		return feature + Lang.DOT + space.getNamespace () + Lang.DOT + name;
	}

	private String createUrl (JsonObject database) {
		return Protocol.Remote + Json.getString (database, Spec.Host) + Lang.COLON + Json.getInteger (database, Spec.Port, 2424) + Lang.SLASH + Json.getString (database, Spec.Database);
	}
	
	public ODatabaseDocumentTx acquire (ApiSpace space, String name) {
		return ((RecyclablePool)space.getRecyclable (createFactoryKey (name, space))).pool ().acquire ();
	}
	
	class RecyclablePool implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private OPartitionedDatabasePool pool;
		
		public RecyclablePool (OPartitionedDatabasePool pool) {
			this.pool = pool;
		}
		
		@Override
		public void recycle () {
			try {
				pool.close ();
			} catch (Exception ex) {
				// Ignore
			}
		}

		public OPartitionedDatabasePool pool () {
			return (OPartitionedDatabasePool)get ();
		}

		@Override
		public Object get () {
			return pool;
		}

		@Override
		public void set (ApiSpace arg0, ClassLoader arg1, Object... arg2) {
			
		}
		
	}
}

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
package com.bluenimble.platform.plugins.database.rdb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;
import javax.sql.DataSource;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.database.rdb.impls.JpaDatabase;
import com.bluenimble.platform.plugins.database.rdb.impls.JpaMetadata;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class RdbPlugin extends AbstractPlugin {

	private static final long serialVersionUID = -8982987236755823201L;
	
	private static final String 	Vendors 		= "vendors";
	
	private static final String 	DataModels		= "dataModels";
	
	public static final String 		DataFolder		= "DataFolder";

	interface SSLProperties {
		String TrustStore 			= "javax.net.ssl.trustStore";
		String TrustStoreType 		= "javax.net.ssl.trustStoreType";
		String TrustStorePassword 	= "javax.net.ssl.trustStorePassword";
	}
	
	interface JpaProperties {
		String NON_JTA_DATASOURCE 	= "javax.persistence.nonJtaDataSource";
		String VALIDATION_MODE 		= "javax.persistence.validation.mode";
		
		String CLASSLOADER 			= "eclipselink.classloader";
		String TARGET_SERVER 		= "eclipselink.target-server";
		String TARGET_DATABASE 		= "eclipselink.target-database";
		String WEAVING 				= "eclipselink.weaving";
		
		String PERSISTENCE_CONTEXT_FLUSH_MODE
									= "eclipselink.persistence-context.flush-mode";
		String CACHE_SIZE_DEFAULT	= "eclipselink.cache.size.default";
		
		String LOGGING_LEVEL 		= "eclipselink.logging.level";		
	}
	
	private static final JsonObject DefaultJpaProperties = new JsonObject ();
	static {
		DefaultJpaProperties.set (JpaProperties.TARGET_SERVER, "None");
		DefaultJpaProperties.set (JpaProperties.WEAVING, "static");
		DefaultJpaProperties.set (JpaProperties.LOGGING_LEVEL, "WARNING");
		DefaultJpaProperties.set (JpaProperties.PERSISTENCE_CONTEXT_FLUSH_MODE, "COMMIT");
		DefaultJpaProperties.set (JpaProperties.CACHE_SIZE_DEFAULT, "1000");
	}
	
	private String 	feature;
	
	private File 	dataFolder;
	private File 	certsFolder;
	
	interface Spec {
		
		String Vendor		= "vendor";
		String Host 		= "host";
		String Port 		= "port";
		String Database 	= "database";
		String Type 		= "type";
		String AutoCommit 	= "autoCommit";
		
		interface Auth {
			String User 		= "user";
			String Password 	= "password";
		}
		
		interface Properties {
			String DataSource 	= "datasource";
			String Jpa 			= "jpa";
		}
		
		interface Pool {
			String MaximumPoolSize 		= "maximumPoolSize";
			String MaxLifeTime 			= "maxLifeTime";
			String MinimumIdle 			= "minimumIdle";
			String ConnectionTimeout 	= "connectionTimeout";
			String IdleTimeout 			= "idleTimeout";
		}	
		
		String AllowProprietaryAccess
							= "allowProprietaryAccess";
		
	}
	
	private Map<String, DataSourceVendor> vendors = new HashMap<String, DataSourceVendor> ();
		
	@Override
	public void init (ApiServer server) throws Exception {
		
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
				// get registered factory and create an EntityManager instance
				return newDatabase (space, name);
			}
			@Override
			public Plugin implementor () {
				return RdbPlugin.this;
			}
			@Override
			public String provider () {
				return RdbPlugin.this.getNamespace ();
			}
		});
		
	}

	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {

		if (Api.class.isAssignableFrom (target.getClass ())) {
			switch (event) {
				case Start:
					createApiFactories ((Api)target);
					break;
				case Stop:
					destroyApiFactories ((Api)target);
					break;
				default:
					break;
			}
			return;
		}
		
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
	
	private void createClients (ApiSpace space) throws PluginRegistryException {
		
		// create factories
		JsonObject allFeatures = Json.getObject (space.getFeatures (), feature);
		if (Json.isNullOrEmpty (allFeatures)) {
			return;
		}
		
		Iterator<String> keys = allFeatures.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			createClient (space, allFeatures, key, false);
		}
	}
	
	private DataSource createClient (ApiSpace space, JsonObject allFeatures, String name, boolean overwrite) throws PluginRegistryException {
		
		JsonObject feature = Json.getObject (allFeatures, name);
		
		if (!this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
			return null;
		}
		
		JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
		
		if (spec == null) {
			return null;
		}
		
		String dataSourceKey = createKey (name);
		
		tracer ().log (Tracer.Level.Info, "Create datasource {0}", dataSourceKey);
		
		if (space.containsRecyclable (dataSourceKey)) {
			return null;
		}
		
		String sVendor = Json.getString (spec, Spec.Vendor);
		tracer ().log (Tracer.Level.Info, "\tDS Vendor {0}", sVendor);
		
		DataSourceVendor vendor = vendors.get (sVendor);
		if (vendor == null) {
			File vendorHome = new File (home, Vendors + Lang.SLASH + sVendor);
			if (vendorHome.exists ()) {
				try {
					vendor = new DataSourceVendor (vendorHome);
				} catch (Exception e) {
					throw new PluginRegistryException (e.getMessage (), e);
				}
				vendors.put (sVendor, vendor);
			} 
		}
		
		tracer ().log (Tracer.Level.Info, "\tDS Vendor Instance {0}", vendor);
		
		if (vendor == null) {
			return null;
		}
		
		DataSource datasource = null;
		
		ClassLoader currentClassLoader = Thread.currentThread ().getContextClassLoader ();
		
		Thread.currentThread ().setContextClassLoader (vendor.classLoader ());
		try {
			HikariConfig config = new HikariConfig ();
			config.setPoolName (dataSourceKey);
			config.setDriverClassName (vendor.driver ());
			
			String url = vendor.url (
				Json.getString (spec, Spec.Host), 
				Json.getInteger (spec, Spec.Port, 0), 
				Json.getString (spec, Spec.Database),
				Json.getString (spec, Spec.Type),
				new File (dataFolder, space.getNamespace () + Lang.UNDERSCORE + name)
			);
			
			config.setJdbcUrl (url);
			
			JsonObject auth = Json.getObject (spec, Spec.Auth.class.getSimpleName ().toLowerCase ());
			if (!Json.isNullOrEmpty (auth)) {
				config.setUsername (Json.getString (auth, Spec.Auth.User));
				config.setPassword (Json.getString (auth, Spec.Auth.Password));
			}
			
			config.setAutoCommit (Json.getBoolean (spec, Spec.AutoCommit, false));
			
			JsonObject pool = Json.getObject (spec, Spec.Pool.class.getSimpleName ().toLowerCase ());
			config.setMaximumPoolSize 	(Json.getInteger (pool, Spec.Pool.MaximumPoolSize, 10));
			config.setMaxLifetime		(Json.getLong (pool, Spec.Pool.MaxLifeTime, 30) * 60 * 1000);
			config.setConnectionTimeout	(Json.getLong (pool, Spec.Pool.ConnectionTimeout, 30) * 60 * 1000);
			config.setIdleTimeout		(Json.getLong (pool, Spec.Pool.IdleTimeout, 10) * 60 * 1000);
			
			int minimumIdle = Json.getInteger (pool, Spec.Pool.MinimumIdle, -1);
			if (minimumIdle > 0) {
				config.setMinimumIdle (minimumIdle);
			}
			
			JsonObject props = (JsonObject)Json.find (spec, Spec.Properties.class.getSimpleName ().toLowerCase (), Spec.Properties.DataSource);
			if (!Json.isNullOrEmpty (props)) {
				// SSL
				String trustStore = props.getString (SSLProperties.TrustStore);
				if (Lang.isNullOrEmpty (trustStore)) {
					props.remove (SSLProperties.TrustStore, SSLProperties.TrustStoreType, SSLProperties.TrustStorePassword);
				} else {
					byte [] storeBytes = Base64.decodeBase64 (props.getString (SSLProperties.TrustStore));
					
					// create store file
					File storeFile = new File (certsFolder, space.getNamespace () + Lang.UNDERSCORE + name);
					OutputStream os = null;
					try {
						os = new FileOutputStream (storeFile);
						IOUtils.copy (new ByteArrayInputStream (storeBytes), os);
					} catch (Exception ex) {
						throw new PluginRegistryException (ex.getMessage (), ex);
					} finally {
						IOUtils.closeQuietly (os);
					}
					props.set (SSLProperties.TrustStore, storeFile.getAbsolutePath ());
				}

				Iterator<String> keys = props.keys ();
				while (keys.hasNext ()) {
					String key = keys.next ();
					config.addDataSourceProperty (key, props.get (key));
				}
			}

			datasource = new HikariDataSource (config); 
			
		} catch (Exception ex) {
			tracer ().log (Tracer.Level.Error, ex.getMessage (), ex);
			throw new PluginRegistryException (ex);
		} finally {
			Thread.currentThread ().setContextClassLoader (currentClassLoader);
		}
		
		Map<String, RecyclableEntityManagerFactory> dataModelFactories = null;
				
		if (overwrite) {
			dataModelFactories = removeClient (space, name);
		}
		
		tracer ().log (Tracer.Level.Info, "\tSpace DataSource {0} ==> {1} / {2}", dataSourceKey, datasource, vendor.name ());
		
		RecyclableDataSource rds = new RecyclableDataSource (vendor.name (), datasource, (JsonObject)Json.find (spec, Spec.Properties.class.getSimpleName ().toLowerCase (), Spec.Properties.DataSource));
		
		space.addRecyclable (dataSourceKey, rds);
		
		// reattach 
		if (dataModelFactories != null && !dataModelFactories.isEmpty ()) {
			Set<String> dmKeys = dataModelFactories.keySet ();
			for (String dmKey : dmKeys) {
				RecyclableEntityManagerFactory remf = dataModelFactories.get (dmKey);
				remf.create ();
				rds.addDataModelFactory (dmKey, remf);
			}
		}
		
		feature.set (ApiSpace.Spec.Installed, true);
		
		return datasource;
		
	}
	
	private Map<String, RecyclableEntityManagerFactory> removeClient (ApiSpace space, String featureName) {
		String key = createKey (featureName);
		RecyclableDataSource recyclable = (RecyclableDataSource)space.getRecyclable (key);
		if (recyclable == null) {
			return null;
		}
		// remove from recyclables
		space.removeRecyclable (key);
		// recycle
		recyclable.recycle ();
		
		return recyclable.dataModelFactories ();
	}
	
	private void createApiFactories (Api api) {
		
		ApiSpace space = api.space ();
		
		// initialize any linked datasource
		JsonObject dataModels = Json.getObject (api.getRuntime (), DataModels);
		
		if (Json.isNullOrEmpty (dataModels)) {
			return;
		}
		
		tracer ().log (Tracer.Level.Info, "Create {0} Api DataModel Factories", api.getNamespace ());

		boolean dependencyAdded = false;
		
		Iterator<String> dms = dataModels.keys ();
		while (dms.hasNext ()) {
			String dataModel = dms.next ();
			String dataSource = Json.getString (dataModels, dataModel);
			
			String key = createKey (dataSource);
			RecyclableDataSource recyclable = (RecyclableDataSource)space.getRecyclable (key);
			if (recyclable == null) {
				continue;
			}
			
			// add plugin dependency to api
			if (!dependencyAdded) {
				((PackageClassLoader)api.getClassLoader ()).addDependency (RdbPlugin.class.getClassLoader ());
			}

			RecyclableEntityManagerFactory factory = new RecyclableEntityManagerFactory (api, recyclable.vendor (), dataSource, dataModel);
			
			String dmrKey = createKey (dataSource + Lang.SHARP + dataModel);
			
			space.addRecyclable (dmrKey, factory);
			
			recyclable.addDataModelFactory (dmrKey, factory);
			
			// add to datasource config
			JsonObject dsDef = Json.getObject (Json.getObject (space.getFeatures (), feature), dataSource);
			
			if (dsDef != null) {
				JsonArray models = Json.getArray (dsDef, DataModels);
				if (models == null) {
					models = new JsonArray ();
					dsDef.set (DataModels, models);
				}
				models.add (dataModel);
			}
			
		}
		
	}
	
	private void destroyApiFactories (Api api) {
		
		ApiSpace space = api.space ();
		
		// initialize any linked datasource
		JsonObject dataModels = Json.getObject (api.getRuntime (), DataModels);
		
		if (Json.isNullOrEmpty (dataModels)) {
			return;
		}
		
		tracer ().log (Tracer.Level.Info, "Destroy {0} Api DataModel Factories", api.getNamespace ());

		Iterator<String> dms = dataModels.keys ();
		while (dms.hasNext ()) {
			String dataModel = dms.next ();
			String dataSource = Json.getString (dataModels, dataModel);
			
			String key = createKey (dataSource);
			RecyclableDataSource dsr = (RecyclableDataSource)space.getRecyclable (key);
			if (dsr == null) {
				continue;
			}
			
			String dmfKey = createKey (dataSource + Lang.SHARP + dataModel);
			Recyclable dmr = space.getRecyclable (dmfKey);
			if (dmr == null) {
				continue;
			}
			
			tracer ().log (Tracer.Level.Info, "\tRecycle Factory {0}", dmfKey);
			dmr.recycle ();
			
			dsr.removeDataModelFactory (dmfKey);
			
			// add to datasource config
			JsonObject dsDef = Json.getObject (Json.getObject (space.getFeatures (), feature), dataSource);
			
			if (dsDef != null) {
				JsonArray models = Json.getArray (dsDef, DataModels);
				if (models != null) {
					models.remove (dataModel);
				}
			}
			
		}
		
	}
	
	private String createKey (String name) {
		return feature + Lang.DOT + getNamespace () + Lang.DOT + name;
	}

	class RecyclableDataSource implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private String 		vendor;
		private DataSource 	datasource;
		private JsonObject 	jpaProperties;
		
		private Map<String, RecyclableEntityManagerFactory> dataModelFactories = new ConcurrentHashMap<String, RecyclableEntityManagerFactory> ();
		
		public RecyclableDataSource (String vendor, DataSource datasource, JsonObject jpaProperties) {
			this.vendor 		= vendor;
			this.datasource 	= datasource;
			this.jpaProperties 	= jpaProperties;
		}
		
		@Override
		public void finish () {
		}

		@Override
		public void recycle () {
			if (!dataModelFactories.isEmpty ()) {
				for (RecyclableEntityManagerFactory remf : dataModelFactories.values ()) {
					remf.recycle ();
				}
			}
			
			try {
				((HikariDataSource)datasource).close ();
			} catch (Exception ex) {
				// Ignore
			}
		}

		public String vendor () {
			return vendor;
		}
		
		public JsonObject jpaProperties () {
			return jpaProperties;
		}
		
		public DataSource datasource () {
			return datasource;
		}
		
		public void addDataModelFactory (String key, RecyclableEntityManagerFactory dataModelFactory) {
			dataModelFactories.put (key, dataModelFactory);
		}
		
		public void removeDataModelFactory (String key) {
			dataModelFactories.remove (key);
		}
		
		public Map<String, RecyclableEntityManagerFactory> dataModelFactories () {
			return dataModelFactories;
		}
		
	}

	class RecyclableEntityManagerFactory implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;
		
		private EntityManagerFactory 	factory;
		private JpaMetadata 			metadata;

		private ApiSpace 				space;
		private	ClassLoader 			classLoader;

		private String 					vendor;
		private String 					dataSource;
		private String 					dataModel;
		
		public RecyclableEntityManagerFactory (Api api, String vendor, String dataSource, String dataModel) {
			this.vendor 		= vendor;
			this.dataModel 		= dataModel;
			this.dataSource 	= dataSource;
			this.space 			= api.space ();
			this.classLoader 	= api.getClassLoader ();
			
			create ();
		}

		@Override
		public void finish () {
		}

		@Override
		public void recycle () {
			
			if (factory == null) {
				return;
			}
			
			tracer ().log (Tracer.Level.Info, "\tRecycle Factory {0}", dataSource + Lang.SHARP + dataModel);
			
			ClassLoader currentClassLoader = Thread.currentThread ().getContextClassLoader ();
			
			Thread.currentThread ().setContextClassLoader (classLoader);
			try {
				
				// close
				factory.close ();
				
			} catch (Exception ex) {
				tracer ().log (Tracer.Level.Error, ex.getMessage (), ex);
			} finally {
				Thread.currentThread ().setContextClassLoader (currentClassLoader);
			}
			
			factory = null;
			metadata = null;
		}

		public EntityManagerFactory get () {
			return factory;
		}

		public JpaMetadata metadata () {
			return metadata;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void create () {
			
			tracer ().log (Tracer.Level.Info, "\tFactory Create");
			
			recycle ();
			
			ClassLoader currentClassLoader = Thread.currentThread ().getContextClassLoader ();
			
			tracer ().log (Tracer.Level.Info, "\tSet Classloader");
			
			Thread.currentThread ().setContextClassLoader (classLoader);
			try {
				Map properties = new HashMap ();
				properties.put (JpaProperties.TARGET_DATABASE, vendor);
				properties.put (JpaProperties.CLASSLOADER, classLoader);
				properties.put (JpaProperties.NON_JTA_DATASOURCE, datasource (space, dataSource));
				
				// add global jpa properties
				JsonObject defaultProperties = DefaultJpaProperties.duplicate ();
				RecyclableDataSource dsr = (RecyclableDataSource)space.getRecyclable (createKey (dataSource));
				if (dsr != null && !Json.isNullOrEmpty (dsr.jpaProperties ())) {
					defaultProperties.merge (dsr.jpaProperties ()); 
				}
				
				Set<String> globalKeys = defaultProperties.keySet ();
				for (String key : globalKeys) {
					properties.put (key, defaultProperties.get (key));
				}

				tracer ().log (Tracer.Level.Info, "\tCreate Entity Manager Factory");
				
				factory = Persistence.createEntityManagerFactory (dataModel, properties);
				metadata = new JpaMetadata (factory);
				
				tracer ().log (Tracer.Level.Info, "\tFound Descriptors");
				printDescriptors (factory);
				
			} catch (Exception ex) {
				ex.printStackTrace ();
				throw new RuntimeException (ex.getMessage (), ex);
			} finally {
				Thread.currentThread ().setContextClassLoader (currentClassLoader);
			} 
		}
		
	}

	private DataSource datasource (ApiSpace space, String name) {
		return ((RecyclableDataSource)space.getRecyclable (createKey (name))).datasource ();
	}
	
	private JpaDatabase newDatabase (ApiSpace space, String name) {
		
		RecyclableEntityManagerFactory recyclable = (RecyclableEntityManagerFactory)space.getRecyclable (createKey (name));
		tracer ().log (Tracer.Level.Info, "\tEntityManager -> Recyclable = {0}", recyclable);
		
		EntityManagerFactory factory = recyclable.get ();
		
		tracer ().log (Tracer.Level.Info, "\tEntityManager ->    Factory = {0}", factory);
		
		Object oAllowProprietaryAccess = 
				Json.find (space.getFeatures (), feature, name, ApiSpace.Features.Spec, Spec.AllowProprietaryAccess);
		boolean allowProprietaryAccess = 
				oAllowProprietaryAccess == null || String.valueOf (oAllowProprietaryAccess).equalsIgnoreCase (Lang.TRUE);
		
		EntityManager em = factory.createEntityManager ();
		
		return new JpaDatabase (this.tracer (), em, recyclable.metadata (), allowProprietaryAccess);
	}
	
	private void printDescriptors (EntityManagerFactory factory) {
		// descriptors
		factory.getMetamodel ().getEntities ();
		for (EntityType<?> entity : factory.getMetamodel ().getEntities ()) {
			tracer ().log (Tracer.Level.Info, "\t ClassDescriptor {0}", entity.getName ());
		}
	}
	
	public String getDataFolder () {
		return null;
	}

	public void setDataFolder (String dataFolder) {
		if (Lang.isNullOrEmpty (dataFolder) || Lang.DOT.equals (dataFolder)) {
			this.dataFolder = home;
		}
		this.dataFolder = new File (dataFolder);
	}

	public String getCertsFolder () {
		return null;
	}

	public void setCertsFolder (String certsFolder) {
		if (Lang.isNullOrEmpty (certsFolder) || Lang.DOT.equals (certsFolder)) {
			this.certsFolder = home;
		}
		this.dataFolder = new File (certsFolder);
	}
	
}

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
package com.bluenimble.platform.plugins.datasource;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.datasource.RemoteDataSource;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.datasource.impls.CustomEntityManager;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSourcePlugin extends AbstractPlugin {

	private static final long serialVersionUID = -8982987236755823201L;
	
	private static final String 	Vendors 		= "vendors";
	
	private static final String 	Provider 		= "bnb-ds";

	private String					feature;
	
	private int 					weight;
	
	interface Spec {
		
		String Vendor		= "vendor";
		String Host 		= "host";
		String Port 		= "port";
		String Database 	= "database";
		
		String User 		= "user";
		String Password 	= "password";

		String Properties 	= "properties";
	}
	
	private Map<String, DataSourceVendor> vendors = new HashMap<String, DataSourceVendor> ();
		
	@Override
	public void init (ApiServer server) throws Exception {
		weight = server.weight ();
		
		Feature aFeature = RemoteDataSource.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		// add features
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = 2626039344401539390L;
			@Override
			public Class<?> type () {
				return RemoteDataSource.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				// get registered factory and create an EntityManager instance
				return entityManager (space, name);
			}
			@Override
			public Plugin implementor () {
				return DataSourcePlugin.this;
			}
			@Override
			public String provider () {
				return Provider;
			}
		});
		
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
				createDataSources (space);
				break;
			case AddFeature:
				createDataSources (space);
				break;
			case DeleteFeature:
				dropDataSources (space);
				break;
			default:
				break;
		}
	}
	
	private void createDataSources (ApiSpace space) throws PluginRegistryException {
		
		// create factories
		JsonObject dbFeature = Json.getObject (space.getFeatures (), feature);
		if (dbFeature == null || dbFeature.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = dbFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject source = Json.getObject (dbFeature, key);
			
			if (!Provider.equalsIgnoreCase (Json.getString (source, ApiSpace.Features.Provider))) {
				continue;
			}
			
			JsonObject spec = Json.getObject (source, ApiSpace.Features.Spec);
			
			if (spec == null) {
				continue;
			}
			
			createDataSource (key, space, spec);
		}
	}
	
	private DataSource createDataSource (String name, ApiSpace space, JsonObject spec) throws PluginRegistryException {
		
		String dataSourceKey = dataSourceKey (name, space);
		
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
			config.setJdbcUrl (vendor.url (Json.getString (spec, Spec.Host), Json.getInteger (spec, Spec.Port, 0), Json.getString (spec, Spec.Database)));
			config.setUsername (Json.getString (spec, Spec.User));
			config.setPassword (Json.getString (spec, Spec.Password));
			config.setAutoCommit (false);
			config.setMaximumPoolSize (weight);
			
			JsonObject props = Json.getObject (spec, Spec.Properties);
			if (!Json.isNullOrEmpty (props)) {
				Iterator<String> keys = props.keys ();
				while (keys.hasNext ()) {
					String key = keys.next ();
					config.addDataSourceProperty (key, props.get (key));
				}
			}

			datasource = new HikariDataSource (config); 
			
		} finally {
			Thread.currentThread ().setContextClassLoader (currentClassLoader);
		}
		
		tracer ().log (Tracer.Level.Info, "\tSpace DataSource {0}", datasource);
		
		space.addRecyclable (dataSourceKey, new RecyclableDataSource (datasource));
		space.addRecyclable (factoryKey (name, space), new RecyclableEntityManagerFactory (null));
		
		return datasource;
		
	}
	
	private void dropDataSources (ApiSpace space) {
		
		JsonObject dbFeature = Json.getObject (space.getFeatures (), feature);
		
		Set<String> recyclables = space.getRecyclables ();
		for (String r : recyclables) {
			if (!r.startsWith (feature + Lang.DOT)) {
				continue;
			}
			String name = r.substring ((feature + Lang.DOT).length ());
			if (dbFeature == null || dbFeature.containsKey (name)) {
				// it's deleted
				RecyclableDataSource rf = (RecyclableDataSource)space.getRecyclable (r);
				// remove from recyclables
				space.removeRecyclable (r);
				// recycle
				rf.recycle ();
			}
		}
		
	}
	
	private String factoryKey (String name, ApiSpace space) {
		return feature + Lang.DOT + space.getNamespace () + Lang.DOT + name;
	}

	private String dataSourceKey (String name, ApiSpace space) {
		return space.getNamespace () + Lang.DOT + name;
	}

	class RecyclableDataSource implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private DataSource datasource;
		
		public RecyclableDataSource (DataSource datasource) {
			this.datasource = datasource;
		}
		
		@Override
		public void recycle () {
			try {
				((HikariDataSource)datasource).close ();
			} catch (Exception ex) {
				// Ignore
			}
		}

		public DataSource get () {
			return datasource;
		}

		@Override
		public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
			
		}
		
	}

	class RecyclableEntityManagerFactory implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private EntityManagerFactory factory;
		
		public RecyclableEntityManagerFactory (EntityManagerFactory factory) {
			this.factory = factory;
		}
		
		@Override
		public void recycle () {
			if (factory == null) {
				return;
			}
			try {
				((EntityManagerFactory)factory).close ();
			} catch (Exception ex) {
				// Ignore
			}
		}

		public EntityManagerFactory get () {
			return factory;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
			ClassLoader currentClassLoader = Thread.currentThread ().getContextClassLoader ();
			
			Thread.currentThread ().setContextClassLoader (DataSourcePlugin.class.getClassLoader ());
			try {
				String dsName = (String)args [0];
				Map properties = new HashMap ();
				properties.put (PersistenceUnitProperties.NON_JTA_DATASOURCE, datasource (space, dsName));
				properties.put (PersistenceUnitProperties.CLASSLOADER, classLoader);
				factory = Persistence.createEntityManagerFactory (dsName, properties);			
			} finally {
				Thread.currentThread ().setContextClassLoader (currentClassLoader);
			}
			
		}
		
	}

	public DataSource datasource (ApiSpace space, String name) {
		return ((RecyclableDataSource)space.getRecyclable (dataSourceKey (name, space))).get ();
	}
	
	public EntityManager entityManager (ApiSpace space, String name) {
		
		RecyclableEntityManagerFactory recyclable = (RecyclableEntityManagerFactory)space.getRecyclable (factoryKey (name, space));
		tracer ().log (Tracer.Level.Debug, "\tEntityManager -> Recyclable = {0}", recyclable);
		
		EntityManagerFactory factory = recyclable.get ();
		
		tracer ().log (Tracer.Level.Debug, "\tEntityManager ->    Factory = {0}", factory);
		
		return new CustomEntityManager (factory.createEntityManager ());
	}
	
}

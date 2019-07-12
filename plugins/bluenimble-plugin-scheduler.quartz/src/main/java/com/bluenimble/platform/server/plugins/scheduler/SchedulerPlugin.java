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
package com.bluenimble.platform.server.plugins.scheduler;

import java.util.Iterator;

import org.quartz.Scheduler;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiSpace.Features;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.impls.DefaultApiContext;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.scheduler.SchedulerException;
import com.bluenimble.platform.scheduler.impls.quartz.QuartzScheduler;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

public class SchedulerPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	interface Prefix {
		String Scheduler 	= "scheduler-";
		String Group 		= "grp-";
		String Job 			= "job-";
		String Trigger 		= "trg-";
	}
	
	public interface Spec {
		String Pool				= "pool";
			String PoolSize		= "size";
			String Priority		= "priority";
			
		String TimeZone 		= "timeZone";
		
		String Jobs 			= "jobs";
			String LocalJobs 	= "local";
			String DatabaseJobs	= "database";
			
		interface Service {
			String Type			= "type";
			String Feature 		= "feature";
			String Runtime 		= "runtime";
			String Request 		= "request";
			String Command 		= "command";
		}
		
		interface Job {
			String Id = "id";
			String Expression 	= "expression";
			String Service		= "service";
			String Space		= "space";
		}
		
		interface DatabaseJob {
			String Feature 		= "feature";
			String Entity 		= "entity";
			String Fields 		= "fields";

			String Node 		= "node";
			String Scheduler 	= "scheduler";
			
			interface Queries {
				String Get 		= "get";
				String All 		= "all";
				String Running 	= "running";
				String Paused 	= "paused";
			}
			
			interface Actions {
				String Create	= "create";
				String Pause	= "pause";
				String Resume	= "resume";
			}
		}
		
	}
	
	private static final ApiContext DefaultApiContext = new DefaultApiContext ();
	
	private DirectSchedulerFactory 	schedulerFactory;
	
	private String 					feature;
	
	private ApiServer 				server;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		this.server = server;
		
		Feature aFeature = com.bluenimble.platform.scheduler.Scheduler.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}

		feature = aFeature.name ();
		
		schedulerFactory = DirectSchedulerFactory.getInstance ();
		
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = 3585173809402444745L;
			@Override
			public String id () {
				return null;
			}
			@Override
			public Class<?> type () {
				return com.bluenimble.platform.scheduler.Scheduler.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return ((RecyclableScheduler)(space.getRecyclable (createKey (name)))).scheduler ();
			}
			@Override
			public String provider () {
				return SchedulerPlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return SchedulerPlugin.this;
			}
		});
		
	}
	
	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Create:
				createSchedulers (space);
				break;
			case AddFeature:
				createScheduler (space, Json.getObject (space.getFeatures (), feature), (String)args [0], (Boolean)args [1]);
				break;
			case DeleteFeature:
				removeScheduler (space, (String)args [0]);
				break;
			default:
				break;
		}
	}
	
	private void createSchedulers (ApiSpace space) throws PluginRegistryException {
		// create sessions
		JsonObject allFeatures = Json.getObject (space.getFeatures (), feature);
		if (Json.isNullOrEmpty (allFeatures)) {
			return;
		}
		
		Iterator<String> keys = allFeatures.keys ();
		while (keys.hasNext ()) {
			createScheduler (space, allFeatures, keys.next (), false);
		}
		
	}
	
	private void createScheduler (ApiSpace space, JsonObject allFeatures, String name, boolean overwrite) throws PluginRegistryException {
		
		JsonObject feature = Json.getObject (allFeatures, name);
		
		if (!this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
			return;
		}
		
		String schedulerKey = createKey (name);
		if (space.containsRecyclable (schedulerKey)) {
			return;
		}
		
		JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
		if (Json.isNullOrEmpty (spec)) {
			return;
		}
		
		JsonObject oPool = Json.getObject (spec, Spec.Pool);
	
		SimpleThreadPool threadPool = new SimpleThreadPool (
			Json.getInteger (oPool, Spec.PoolSize, 4),
			Json.getInteger (oPool, Spec.Priority, Thread.NORM_PRIORITY)
			
		);
		
		if (overwrite) {
			removeScheduler (space, name);
		}
		
		String schedulerId = Prefix.Scheduler + space.getNamespace () + Lang.DOT + name;
		
        threadPool.setThreadNamePrefix (space.getNamespace () + Lang.DOT + name);
        threadPool.setThreadsInheritContextClassLoaderOfInitializingThread (true);
        
        Scheduler qScheduler = null;
        
        try {
            threadPool.initialize ();
            schedulerFactory.createScheduler (
            	schedulerId, 
            	schedulerId, 
            	threadPool, 
            	new RAMJobStore ()
            );
            
            qScheduler = schedulerFactory.getScheduler (schedulerId);
        } catch (Exception e) {
        	 throw new PluginRegistryException (e);
        }
        
        QuartzScheduler scheduler = new QuartzScheduler (space, server.id (), schedulerId, spec, qScheduler);
        
        try {
			scheduler.start ();
		} catch (SchedulerException e) {
			throw new PluginRegistryException (e);
		}

		// load local and remote jobs
        loadJobs (space, scheduler, name, spec);
		
		space.addRecyclable (
			schedulerKey, 
			new RecyclableScheduler (scheduler)
		);
	
		feature.set (ApiSpace.Spec.Installed, true);
	}
	
	private void removeScheduler (ApiSpace space, String featureName) {
		String key = createKey (featureName);
		Recyclable recyclable = space.getRecyclable (createKey (featureName));
		if (recyclable == null) {
			return;
		}
		// remove from recyclables
		space.removeRecyclable (key);
		// recycle
		recyclable.recycle ();
	}
	
	private String createKey (String name) {
		return feature + Lang.DOT + getNamespace () + Lang.DOT + name;
	}
	
	private void loadJobs (ApiSpace space, QuartzScheduler scheduler, String feature, JsonObject spec) throws PluginRegistryException {
		
		JsonObject jobs = Json.getObject (spec, Spec.Jobs);
		if (Json.isNullOrEmpty (jobs)) {
			return;
		}
		
		JsonObject localJobs = Json.getObject (jobs, Spec.LocalJobs);
		if (!Json.isNullOrEmpty (localJobs)) {
			Iterator<String> ids = localJobs.keys ();
			while (ids.hasNext ()) {
				String jobId = ids.next ();
				JsonObject oJob  = Json.getObject (localJobs, jobId);
				try {
					scheduler.schedule (
						jobId,
						Json.getString (oJob, Spec.Job.Expression), 
						Json.getObject (oJob, Spec.Job.Service),
						false
					);
				} catch (Exception ex) {
					throw new PluginRegistryException (ex.getMessage (), ex);
				}
			}
		}
		
		JsonObject databaseJobs = Json.getObject (jobs, Spec.DatabaseJobs);
		if (Json.isNullOrEmpty (databaseJobs)) {
			return;
		}
		
		JsonObject query = (JsonObject)Json.find (
			databaseJobs, 
			Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.Running
		);
		
		if (Json.isNullOrEmpty (query)) {
			return;
		}
		
		JsonObject tplData = new JsonObject ();
		tplData.set (Spec.DatabaseJob.Node, server.id ());
		tplData.set (Spec.DatabaseJob.Scheduler, feature);
		
		query = Json.template (query, tplData, false);
		
		JsonObject fields = Json.getObject (databaseJobs, Spec.DatabaseJob.Fields);
		
		boolean withError = true;
		
		try {
			Database db = space.feature (Database.class, Json.getString (databaseJobs, Spec.DatabaseJob.Feature, Features.Default), DefaultApiContext);
			
			db.find (
				Json.getString (databaseJobs, Spec.DatabaseJob.Entity), 
				new JsonQuery (query),
				new Database.Visitor () {
					@Override
					public boolean onRecord (DatabaseObject dbo) {
						try {
							scheduler.schedule (
								(String)dbo.get (Json.getString (fields, Spec.Job.Id, Spec.Job.Id)), 
								(String)dbo.get (Json.getString (fields, Spec.Job.Expression, Spec.Job.Expression)), 
								(JsonObject)dbo.get (Json.getString (fields, Spec.Job.Service, Spec.Job.Service)),
								false
							);
						} catch (SchedulerException e) {
							throw new RuntimeException (e.getMessage (), e);
						}
						return false;
					}
					@Override
					public boolean optimize () {
						return true;
					}
				}
			);
			withError = false;
		} catch (Exception ex) {
			throw new PluginRegistryException (ex.getMessage (), ex);
		} finally {
			DefaultApiContext.finish (withError);
			DefaultApiContext.recycle ();
		}
	}

	class RecyclableScheduler implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private QuartzScheduler scheduler;
		
		public RecyclableScheduler (QuartzScheduler scheduler) {
			this.scheduler = scheduler;
		}
		
		@Override
		public void finish (boolean withError) {
			// nothing
		}

		@Override
		public void recycle () {
			scheduler.stop ();
		}

		public QuartzScheduler scheduler () {
			return scheduler;
		}
		
	}
	
	public ApiServer server () {
		return server;
	}

}

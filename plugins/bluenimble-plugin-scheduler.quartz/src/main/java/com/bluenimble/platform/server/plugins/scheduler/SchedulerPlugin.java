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

import static org.quartz.impl.matchers.EverythingMatcher.allTriggers;

import java.util.Date;
import java.util.Iterator;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;
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
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.scheduler.Scheduler.SchedulingResult;
import com.bluenimble.platform.scheduler.SchedulerException;
import com.bluenimble.platform.scheduler.impls.quartz.JobTask;
import com.bluenimble.platform.scheduler.impls.quartz.QuartzScheduler;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

public class SchedulerPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private static final JobTask JobTaskUnschedule 				= 
			new JobTask (SchedulerPlugin.Spec.Lifecycle.Action.Unschedule);
	private static final JsonObject JobTaskUnscheduleRuntime 	= 
			(JsonObject)new JsonObject ().set (JobTask.Spec.Action, Spec.Lifecycle.Action.Unschedule);
	
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
			
		interface Lifecycle {
			interface Action {
				String Schedule		= "schedule";
				String Tick			= "tick";
				String Unschedule	= "unschedule";
			}
			String Type			= "type";
			String Feature 		= "feature";
			String Runtime 		= "runtime";
			String Request 		= "request";
			String Command 		= "command";
		}
		
		interface Job {
			String Id = "id";
			String Scheduler 	= "scheduler";
			String Expression 	= "expression";
			String Lifecycle	= "lifecycle";
			String Space		= "space";
			String Metadata		= "metadata";
			String StartTime 	= "startTime";
			String EndTime 		= "endTime";
			String Running 		= "running";
		}
		
		interface DatabaseJob {
			String Feature 		= "feature";
			String Entity 		= "entity";
			String Fields 		= "fields";
			String Job 			= "job";

			String Node 		= "node";
			
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
				createScheduler (space, Json.getObject (space.getFeatures (), feature), (String)args [0], (Boolean)args [1], false);
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
			createScheduler (space, allFeatures, keys.next (), false, true);
		}
		
	}
	
	private void createScheduler (ApiSpace space, JsonObject allFeatures, String name, boolean overwrite, boolean isStartupTime) throws PluginRegistryException {
		
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
        
        final QuartzScheduler scheduler = new QuartzScheduler (space, server.id (), schedulerId, spec, qScheduler);
        
        try {
            // add ending listener
        	scheduler.oScheduler.getListenerManager().addTriggerListener (new TriggerListener () {
				@Override
				public String getName () {
					return "EndedTriggerListener";
				}
				@Override
				public void triggerFired (Trigger trigger, JobExecutionContext context) { }
				@Override
				public boolean vetoJobExecution (Trigger trigger, JobExecutionContext context) { return false; }
				@Override
				public void triggerMisfired (Trigger trigger) { }
				@Override
				public void triggerComplete (Trigger trigger, JobExecutionContext context,
						CompletedExecutionInstruction triggerInstructionCode) {
					JobDataMap jobDataMap = context.getJobDetail ().getJobDataMap ();
					String jobId = jobDataMap.getString (Spec.Job.Id);
					space.tracer ().log (Level.Info, "Calling TriggerListener On Job {0}", jobId);
					space.tracer ().log (Level.Info, "Trigger EndTime {0}", trigger.getEndTime ());
					if (trigger.getEndTime () != null && trigger.getEndTime ().before (new Date ())) {
						space.tracer ().log (Level.Info, " -> TriggerListener - Unschedule {0}", jobId);
						try {
							scheduler.unschedule (jobId, true);
							JobTaskUnschedule.execute (context);
						} catch (SchedulerException e) {
							throw new RuntimeException (e.getMessage (), e);
						} catch (JobExecutionException e) {
							space.tracer ().log (Level.Warning, "Ending Job Error", e);
						}
					}
				}
    		}, allTriggers ());
            
			scheduler.start ();
			
		} catch (Exception e) {
			throw new PluginRegistryException (e);
		}

		space.addRecyclable (
			schedulerKey, 
			new RecyclableScheduler (scheduler)
		);
	
		feature.set (ApiSpace.Spec.Installed, true);

		// load local and remote jobs
        loadJobs (space, scheduler, name, spec, isStartupTime);
		
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
	
	private void loadJobs (ApiSpace space, QuartzScheduler scheduler, String feature, JsonObject spec, boolean isStartupTime) throws PluginRegistryException {
		
		JsonObject jobs = Json.getObject (spec, Spec.Jobs);
		if (Json.isNullOrEmpty (jobs)) {
			return;
		}
		
		JsonObject localJobs = Json.getObject (jobs, Spec.LocalJobs);
		if (!Json.isNullOrEmpty (localJobs)) {
			Iterator<String> ids = localJobs.keys ();
			while (ids.hasNext ()) {
				String jobId = ids.next ();
				JsonObject oJob  = Json.getObject (localJobs, jobId).duplicate ();
				oJob.set (Spec.Job.Id, jobId);
				try {
					scheduler.schedule (oJob, false);
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
			Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.All
		);
		
		if (Json.isNullOrEmpty (query)) {
			return;
		}
		
		JsonObject tplData = new JsonObject ();
		tplData.set (Spec.DatabaseJob.Node, server.id ());
		
		query = Json.template (query, tplData, false);
		
		loadDatabaseJobs (space, scheduler, query, databaseJobs, isStartupTime ? 0 : -1);
	}
	
	private void loadDatabaseJobs (ApiSpace space, QuartzScheduler scheduler, JsonObject query, JsonObject databaseJobs, int timeout) throws PluginRegistryException {
		
		JsonObject fields = Json.getObject (databaseJobs, Spec.DatabaseJob.Fields);
		
		boolean withError = true;
		
		Database db = null;
		try {
			try {
				db = space.feature (Database.class, Json.getString (databaseJobs, Spec.DatabaseJob.Feature, Features.Default), DefaultApiContext);
			} catch (Exception ex) {
				// Ignore
			}
			if (db == null && timeout > -1) {
				if (timeout >= 30000) {
					throw new PluginRegistryException ("Scheduler Jobs Database feature not found");
				}
				try {
			        Thread.sleep (3000);
			    } catch (InterruptedException ex) {
			        Thread.currentThread ().interrupt ();
			    }
				this.loadDatabaseJobs (space, scheduler, query, databaseJobs, timeout + 3000);
				return;
			}
			db.find (
				Json.getString (databaseJobs, Spec.DatabaseJob.Entity),
				new JsonQuery (query),
				new Database.Visitor () {
					@Override
					public boolean onRecord (DatabaseObject dbo) {
						JsonObject job = new JsonObject ();
						job.set (Spec.Job.Id, (String)dbo.get (Json.getString (fields, Spec.Job.Id, Spec.Job.Id)));
						job.set (Spec.Job.Expression, (String)dbo.get (Json.getString (fields, Spec.Job.Expression, Spec.Job.Expression)));
						job.set (Spec.Job.Lifecycle, (JsonObject)dbo.get (Json.getString (fields, Spec.Job.Lifecycle, Spec.Job.Lifecycle)));
						job.set (Spec.Job.StartTime, (String)dbo.get (Json.getString (fields, Spec.Job.StartTime, Spec.Job.StartTime)));
						job.set (Spec.Job.EndTime, (String)dbo.get (Json.getString (fields, Spec.Job.EndTime, Spec.Job.EndTime)));
						SchedulingResult scheduled = SchedulingResult.Unknown;
						try {
							scheduled = scheduler.schedule (job, false);
						} catch (SchedulerException e) {
							throw new RuntimeException (e.getMessage (), e);
						}
						try {
							if (scheduled.equals (SchedulingResult.Expired)) {
								dbo.delete ();
								JobTaskUnschedule.process (
									space, 
									Json.getObject (job, Spec.Job.Lifecycle), 
									JobTaskUnscheduleRuntime
								);
							}
						} catch (Exception e) {
							space.tracer().log (Level.Error, "Deleting Expired Job Error", e);
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

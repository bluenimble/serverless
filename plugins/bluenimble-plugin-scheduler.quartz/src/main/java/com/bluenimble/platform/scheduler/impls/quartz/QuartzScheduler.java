package com.bluenimble.platform.scheduler.impls.quartz;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiSpace.Features;
import com.bluenimble.platform.api.impls.DefaultApiContext;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.scheduler.Scheduler;
import com.bluenimble.platform.scheduler.SchedulerException;
import com.bluenimble.platform.server.plugins.scheduler.SchedulerPlugin;
import com.bluenimble.platform.server.plugins.scheduler.SchedulerPlugin.Spec;

public class QuartzScheduler implements Scheduler {

	private static final long serialVersionUID = 8183873792425626789L;
	
	interface Prefix {
		String Scheduler 	= "scheduler-";
		String Trigger 		= "trg-";
		String Job 			= "job-";
	}
	
	interface Output {
		String 		Jobs = "jobs";
		interface 	Trigger {
			String Last 	= "last";
			String Next 	= "next";
			String End 		= "end";
		}
	}

	private ApiSpace 				space;
	private String 					node;
	private String 					id;
	private org.quartz.Scheduler 	oScheduler;
	private JsonObject 				spec;

	public QuartzScheduler (ApiSpace space, String node, String id, JsonObject spec, org.quartz.Scheduler oScheduler) {
		this.space 		= space;
		this.node 		= node;
		this.id 		= id;
		this.spec 		= spec;
		this.oScheduler = oScheduler;
	}
	
	@Override
	public void start () throws SchedulerException {
		try {
			if (oScheduler.isStarted ()) {
				return;
			}
			oScheduler.start ();
		} catch (org.quartz.SchedulerException e) {
			throw new SchedulerException (e.getMessage (), e);
		}
	}
	
	@Override
	public void stop () {
		try {
			if (oScheduler.isShutdown ()) {
				return;
			}
			oScheduler.shutdown ();
		} catch (org.quartz.SchedulerException e) {
			space.tracer ().log (Level.Error, "Scheduler Shutdown Error", e);
		}
	}
	
	@Override
	public void schedule (String id, String expression, JsonObject service, boolean save) throws SchedulerException {
		
		if (!save) {
			try {
				doSchedule (id, expression, service);
			} catch (Exception e) {
				throw new SchedulerException (e.getMessage (), e);
			} 
			return;
		}
		
		// persist if having a databaseJobs
		JsonObject databaseJobs = (JsonObject)Json.find (spec, SchedulerPlugin.Spec.Jobs, SchedulerPlugin.Spec.DatabaseJobs);
		if (Json.isNullOrEmpty (databaseJobs)) {
			throw new SchedulerException ("No Persistent Jobs config found");
		}
		
		space.tracer ().log (Level.Info, "Save Job {0} State", save);
		
		ApiContext context = new DefaultApiContext ();
		boolean withError = true;
		try {
			Database db = space.feature (
				Database.class, 
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Feature, Features.Default), 
				context
			).trx ();
			
			JsonObject tplData = (JsonObject)new JsonObject ()
					.set (SchedulerPlugin.Spec.DatabaseJob.Node, node)
					.set (SchedulerPlugin.Spec.DatabaseJob.Scheduler, this.id)
					.set (SchedulerPlugin.Spec.Job.Id, id);
			
			JsonObject getQuery = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.Get
			);

			if (!Json.isNullOrEmpty (getQuery)) {
				DatabaseObject dbo = db.findOne (
					Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity),
					new JsonQuery (getQuery)
				);
				if (dbo != null) {
					throw new SchedulerException ("Job " + node + "/" + id + " already exist");
				}
			}
			
			tplData
				.set (SchedulerPlugin.Spec.Job.Expression, expression)
				.set (SchedulerPlugin.Spec.Job.Service, service);
			
			JsonObject createAction = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Actions.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Actions.Create
			);
			
			JsonObject values = Json.template (createAction, tplData, false);
			
			space.tracer ().log (Level.Info, "Create DB CronJon with values: {0}", values);
			
			DatabaseObject dbo = db.create (Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity));
			dbo.load (values);
			dbo.save ();
			
			// schedule job
			doSchedule (id, expression, service);
			
			withError = false;
		} catch (Exception e) {
			throw new SchedulerException (e.getMessage (), e);
		} finally {
			context.finish (withError);
			context.recycle ();
		}
		
	}

	@Override
	public void unschedule (String id, boolean save) throws SchedulerException {
		if (!save) {
			try {
				doUnschedule (id);
			} catch (Exception e) {
				throw new SchedulerException (e.getMessage (), e);
			} 
			return;
		}
		
		space.tracer ().log (Level.Info, "Save Job {0} State", save);
		
		// persist if having a databaseJobs
		JsonObject databaseJobs = (JsonObject)Json.find (spec, SchedulerPlugin.Spec.Jobs, SchedulerPlugin.Spec.DatabaseJobs);
		if (Json.isNullOrEmpty (databaseJobs)) {
			throw new SchedulerException ("No Persistent Jobs config found");
		}
		
		ApiContext context = new DefaultApiContext ();
		boolean withError = true;
		try {
			Database db = space.feature (
				Database.class, 
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Feature, Features.Default), 
				context
			);
			
			JsonObject getQuery = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.Get
			);
			if (!Json.isNullOrEmpty (getQuery)) {
				getQuery = Json.template (
					getQuery,
					(JsonObject)new JsonObject ()
						.set (SchedulerPlugin.Spec.DatabaseJob.Node, node)
						.set (SchedulerPlugin.Spec.DatabaseJob.Scheduler, this.id)
						.set (SchedulerPlugin.Spec.Job.Id, id),
					false
				);
			
				DatabaseObject dbo = db.findOne (
					Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity),
					new JsonQuery (getQuery)
				);
				
				if (dbo != null) {
					dbo.delete ();
				}
			}
			
			doUnschedule (id);
			withError = false;
		} catch (Exception e) {
			throw new SchedulerException (e.getMessage (), e);
		} finally {
			context.finish (withError);
			context.recycle ();
		}
	}
	
	@Override
	public void pause (String id, boolean save) throws SchedulerException {
		if (!save) {
			try {
				doPause (id);
			} catch (Exception e) {
				throw new SchedulerException (e.getMessage (), e);
			} 
			return;
		}
		
		space.tracer ().log (Level.Info, "Save Job State / Pause => {0}", save);
		
		// persist if having a databaseJobs
		JsonObject databaseJobs = (JsonObject)Json.find (spec, SchedulerPlugin.Spec.Jobs, SchedulerPlugin.Spec.DatabaseJobs);
		if (Json.isNullOrEmpty (databaseJobs)) {
			throw new SchedulerException ("No Persistent Jobs config found");
		}
		
		ApiContext context = new DefaultApiContext ();
		boolean withError = true;
		try {
			Database db = space.feature (
				Database.class, 
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Feature, Features.Default), 
				context
			).trx ();
			
			JsonObject tplData = (JsonObject)new JsonObject ()
					.set (SchedulerPlugin.Spec.DatabaseJob.Node, node)
					.set (SchedulerPlugin.Spec.DatabaseJob.Scheduler, this.id)
					.set (SchedulerPlugin.Spec.Job.Id, id);
			
			JsonObject getQuery = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.Get
			);
			
			getQuery = Json.template (getQuery, tplData, false);

			DatabaseObject dbo = null;
			if (!Json.isNullOrEmpty (getQuery)) {
				dbo = db.findOne (
					Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity),
					new JsonQuery (getQuery)
				);
			}
			
			if (dbo == null) {
				throw new SchedulerException ("Job " + node + "/" + id + " not found");
			}
			
			JsonObject pauseAction = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Actions.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Actions.Pause
			);
			
			JsonObject values = Json.template (pauseAction, tplData, false);
			
			space.tracer ().log (Level.Info, "Pause DB CronJon with values: {0}", values);
			
			dbo.load (values);
			dbo.save ();
			
			// schedule job
			doPause (id);
			
			withError = false;
		} catch (Exception e) {
			throw new SchedulerException (e.getMessage (), e);
		} finally {
			context.finish (withError);
			context.recycle ();
		}
	}
	
	@Override
	public void resume (String id, boolean save) throws SchedulerException {
		if (!save) {
			try {
				doResume (id);
			} catch (Exception e) {
				throw new SchedulerException (e.getMessage (), e);
			} 
			return;
		}
		
		space.tracer ().log (Level.Info, "Save Job State / Pause => {0}", save);
		
		// persist if having a databaseJobs
		JsonObject databaseJobs = (JsonObject)Json.find (spec, SchedulerPlugin.Spec.Jobs, SchedulerPlugin.Spec.DatabaseJobs);
		if (Json.isNullOrEmpty (databaseJobs)) {
			throw new SchedulerException ("No Persistent Jobs config found");
		}
		
		ApiContext context = new DefaultApiContext ();
		boolean withError = true;
		try {
			Database db = space.feature (
				Database.class, 
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Feature, Features.Default), 
				context
			).trx ();
			
			JsonObject tplData = (JsonObject)new JsonObject ()
					.set (SchedulerPlugin.Spec.DatabaseJob.Node, node)
					.set (SchedulerPlugin.Spec.DatabaseJob.Scheduler, this.id)
					.set (SchedulerPlugin.Spec.Job.Id, id);
			
			JsonObject getQuery = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.Get
			);
			getQuery = Json.template (getQuery, tplData, false);

			DatabaseObject dbo = null;
			if (!Json.isNullOrEmpty (getQuery)) {
				dbo = db.findOne (
					Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity),
					new JsonQuery (getQuery)
				);
			}
			
			if (dbo == null) {
				throw new SchedulerException ("Job " + node + "/" + id + " not found");
			}
			
			JsonObject resumeAction = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Actions.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Actions.Resume
			);
			
			JsonObject values = Json.template (resumeAction, tplData, false);
			
			space.tracer ().log (Level.Info, "Resume DB CronJon with values: {0}", values);
			
			dbo.load (values);
			dbo.save ();
			
			// schedule job
			doResume (id);
			
			withError = false;
		} catch (Exception e) {
			throw new SchedulerException (e.getMessage (), e);
		} finally {
			context.finish (withError);
			context.recycle ();
		}
	}
	
	@Override
	public JsonObject get (String id) throws SchedulerException {
		// persist if having a databaseJobs
		JsonObject databaseJobs = (JsonObject)Json.find (spec, SchedulerPlugin.Spec.Jobs, SchedulerPlugin.Spec.DatabaseJobs);
		if (Json.isNullOrEmpty (databaseJobs)) {
			throw new SchedulerException ("No Persistent Jobs config found");
		}
		
		JsonObject getQuery = (JsonObject)Json.find (
			databaseJobs, 
			Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.Get
		);
		if (Json.isNullOrEmpty (getQuery)) {
			throw new SchedulerException ("No Persistent Jobs Get Query config found");
		}
		
		DatabaseObject dbo = null;
		
		ApiContext context = new DefaultApiContext ();
		try {
			Database db = space.feature (
				Database.class, 
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Feature, Features.Default), 
				context
			);
			
			JsonObject tplData = (JsonObject)new JsonObject ()
					.set (SchedulerPlugin.Spec.DatabaseJob.Node, node)
					.set (SchedulerPlugin.Spec.DatabaseJob.Scheduler, this.id)
					.set (SchedulerPlugin.Spec.Job.Id, id);
			
			getQuery = Json.template (getQuery, tplData, false);
			
			dbo = db.findOne (
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity),
				new JsonQuery (getQuery)
			);			
		} catch (DatabaseException dbex) {
			throw new SchedulerException (dbex.getMessage (), dbex);
		}
		
		if (dbo == null) {
			return null;
		}
		
		JsonObject record = dbo.toJson (null);
		
		String jobIdField = (String)Json.find (databaseJobs, Spec.DatabaseJob.Fields, Spec.Job.Id);
		if (Lang.isNullOrEmpty (jobIdField)) {
			jobIdField = Spec.Job.Id;
		}
		
		String jobId = Json.getString (record, jobIdField);
		record.set (
			Output.Trigger.class.getSimpleName ().toLowerCase (), 
			trigger (jobId)
		);
		record.set (Database.Fields.Id, jobId);
		record.remove (jobIdField);
		
		return record;
		
	}
	
	@Override
	public JsonObject list (int offset, int count, int status) throws SchedulerException {
		// persist if having a databaseJobs
		JsonObject databaseJobs = (JsonObject)Json.find (spec, SchedulerPlugin.Spec.Jobs, SchedulerPlugin.Spec.DatabaseJobs);
		if (Json.isNullOrEmpty (databaseJobs)) {
			throw new SchedulerException ("No Persistent Jobs config found");
		}
		
		String queryId = Spec.DatabaseJob.Queries.All;
		switch (status) {
			case 0:
				queryId = Spec.DatabaseJob.Queries.Paused;
				break;
			case 1:
				queryId = Spec.DatabaseJob.Queries.Running;
				break;
			default:
				break;
		}
		
		JsonObject listQuery = (JsonObject)Json.find (
			databaseJobs, 
			Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), queryId
		);
		if (Json.isNullOrEmpty (listQuery)) {
			throw new SchedulerException ("No Persistent Jobs List Query config found");
		}
		
		JsonObject result 	= new JsonObject ();
		JsonArray 	aJobs 	= new JsonArray ();
		result.set (Output.Jobs, aJobs);
		
		ApiContext context = new DefaultApiContext ();
		try {
			Database db = space.feature (
				Database.class, 
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Feature, Features.Default), 
				context
			);
			
			JsonObject tplData = (JsonObject)new JsonObject ()
					.set (SchedulerPlugin.Spec.DatabaseJob.Node, node)
					.set (SchedulerPlugin.Spec.DatabaseJob.Scheduler, this.id);
			
			listQuery = Json.template (listQuery, tplData, false);
			
			String jobIdField = (String)Json.find (databaseJobs, Spec.DatabaseJob.Fields, Spec.Job.Id);
			if (Lang.isNullOrEmpty (jobIdField)) {
				jobIdField = Spec.Job.Id;
			}
			
			final String jf = jobIdField;
			
			db.find (
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity),
				new JsonQuery (listQuery),
				new Database.Visitor () {
					@Override
					public boolean onRecord (DatabaseObject dbo) {
						try {
							JsonObject record = dbo.toJson (null);
							String jobId = Json.getString (record, jf);
							record.set (
								Output.Trigger.class.getSimpleName ().toLowerCase (), 
								trigger (jobId)
							);
							record.set (Database.Fields.Id, jobId);
							record.remove (jf);
							aJobs.add (record);
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
		} catch (DatabaseException dbex) {
			throw new SchedulerException (dbex.getMessage (), dbex);
		}
		
		return result;
		
	}
	
	private JsonObject trigger (String jobId) throws SchedulerException {
		Trigger trigger = null;
		try {
			trigger = oScheduler.getTrigger (triggerKey (Prefix.Trigger + jobId, this.id));
		} catch (Exception ex) {
			throw new SchedulerException (ex.getMessage (), ex);
		}
		if (trigger == null) {
			return null;
		}
		JsonObject oTrigger = new JsonObject ();
		oTrigger.set (Output.Trigger.Last, trigger.getPreviousFireTime ());
		oTrigger.set (Output.Trigger.Next, trigger.getNextFireTime ());
		oTrigger.set (Output.Trigger.End, trigger.getFinalFireTime ());
		return oTrigger;
	}
	
	private void doSchedule (String id, String expression, JsonObject service) throws Exception {
		space.tracer ().log (Level.Info, "Schedule Job {0}", id);
		JobBuilder builder = newJob (JobTask.class).withIdentity (Prefix.Job + id, this.id);
		
		builder.usingJobData (SchedulerPlugin.Spec.Job.Space, space.getNamespace ());
		builder.usingJobData (SchedulerPlugin.Spec.Job.Service, service.toString (0, true));
		
		JobDetail job = builder.build ();
		
		String timeZone = Json.getString (this.spec, SchedulerPlugin.Spec.TimeZone);
		
		CronScheduleBuilder csb = cronSchedule (expression);
		if (!Lang.isNullOrEmpty (timeZone)) {
			csb.inTimeZone (TimeZone.getTimeZone (timeZone));
		}
		Trigger trigger = newTrigger ()
				.withIdentity (Prefix.Trigger + id, this.id)
				.withSchedule (csb)
			    .build ();
		oScheduler.scheduleJob (job, trigger);
	}

	private void doUnschedule (String id) throws Exception {
		space.tracer ().log (Level.Info, "Unschedule Job {0}", id);
		oScheduler.deleteJob (jobKey (Prefix.Job + id, this.id));
	}
	
	private void doPause (String id) throws Exception {
		space.tracer ().log (Level.Info, "Pause Job {0}", id);
		oScheduler.pauseJob (jobKey (Prefix.Job + id, this.id));
	}
	
	private void doResume (String id) throws Exception {
		space.tracer ().log (Level.Info, "Pause Job {0}", id);
		oScheduler.resumeJob (jobKey (Prefix.Job + id, this.id));
	}
	

}

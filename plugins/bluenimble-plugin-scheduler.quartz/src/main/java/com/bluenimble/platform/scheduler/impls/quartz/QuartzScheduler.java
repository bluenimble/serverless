package com.bluenimble.platform.scheduler.impls.quartz;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

import java.util.Date;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

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
import com.bluenimble.platform.query.Query;
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
	public 	org.quartz.Scheduler 	oScheduler;
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
	public SchedulingResult schedule (JsonObject job, boolean save) throws SchedulerException {
		
		space.tracer ().log (Level.Info, "Schedule Job {0}", job);
		
		if (!save) {
			try {
				return doSchedule (job);
			} catch (Exception e) {
				throw new SchedulerException (e.getMessage (), e);
			} 
		}
		
		job.set (SchedulerPlugin.Spec.Job.Scheduler, node);
		job.set (SchedulerPlugin.Spec.Job.Running, true);
		
		job.shrink ();
		
		// persist if having a databaseJobs
		JsonObject databaseJobs = (JsonObject)Json.find (spec, SchedulerPlugin.Spec.Jobs, SchedulerPlugin.Spec.DatabaseJobs);
		if (Json.isNullOrEmpty (databaseJobs)) {
			throw new SchedulerException ("No Persistent Jobs config found");
		}
		
		space.tracer ().log (Level.Info, "Save Job {0} State", save);
		
		ApiContext context = new DefaultApiContext ();
		boolean withError = true;
		SchedulingResult scheduled = SchedulingResult.Unknown;
		try {
			Database db = space.feature (
				Database.class, 
				Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Feature, Features.Default), 
				context
			).trx ();
			
			JsonObject getQuery = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.Get
			);

			if (!Json.isNullOrEmpty (getQuery)) {
				getQuery = Json.template (
					getQuery, 
					(JsonObject)new JsonObject ()
						.set (SchedulerPlugin.Spec.DatabaseJob.Node, node)
						.set (SchedulerPlugin.Spec.Job.Id, job.get (SchedulerPlugin.Spec.Job.Id)), 
					false
				);
				DatabaseObject dbo = db.findOne (
					Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity),
					new JsonQuery (getQuery)
				);
				if (dbo != null) {
					throw new SchedulerException ("Job " + node + "/" + job.get (SchedulerPlugin.Spec.Job.Id) + " already exist");
				}
			}
			
			job.set (SchedulerPlugin.Spec.DatabaseJob.Job, job.get (SchedulerPlugin.Spec.Job.Id));
			job.remove (SchedulerPlugin.Spec.Job.Id);
			
			DatabaseObject dbo = db.create (Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity));
			dbo.load (job);
			dbo.save ();
			
			// schedule job
			scheduled = doSchedule (job);
			
			if (scheduled.equals (SchedulingResult.Scheduled)) {
				withError = false;
			}
		} catch (Exception e) {
			throw new SchedulerException (e.getMessage (), e);
		} finally {
			context.finish (withError);
			context.recycle ();
		}
		return scheduled;
	}

	@Override
	public void unschedule (String id, boolean save) throws SchedulerException {
		if (Lang.isNullOrEmpty (id)) {
			throw new SchedulerException ("Job Id is required for Unschedule operation");
		}
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
			).trx ();
			
			JsonObject getQuery = (JsonObject)Json.find (
				databaseJobs, 
				Spec.DatabaseJob.Queries.class.getSimpleName ().toLowerCase (), Spec.DatabaseJob.Queries.Get
			);
			if (!Json.isNullOrEmpty (getQuery)) {
				getQuery = Json.template (
					getQuery,
					(JsonObject)new JsonObject ()
						.set (SchedulerPlugin.Spec.DatabaseJob.Node, node)
						.set (SchedulerPlugin.Spec.Job.Id, id),
					false
				);
			
				DatabaseObject dbo = db.findOne (
					Json.getString (databaseJobs, SchedulerPlugin.Spec.DatabaseJob.Entity),
					new JsonQuery (getQuery)
				);
				
				if (dbo != null) {
					space.tracer ().log (Level.Info, "Delete Database Job {0}", dbo.getId ());
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
	public JsonObject list (int offset, int count, int status, JsonObject metaQuery) throws SchedulerException {
		// list only if having a databaseJobs
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
		if (!Json.isNullOrEmpty (metaQuery)) {
			JsonObject where = Json.getObject (listQuery, Query.Construct.where.name ());
			if (where == null) {
				where = new JsonObject ();
				listQuery.set (Query.Construct.where.name (), where);
			}
			where.merge (metaQuery);
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
					.set (SchedulerPlugin.Spec.DatabaseJob.Node, node);
			
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
	
	private SchedulingResult doSchedule (JsonObject oJob) throws Exception {

		if (oJob.containsKey (SchedulerPlugin.Spec.DatabaseJob.Job) && !oJob.containsKey (SchedulerPlugin.Spec.Job.Id)) {
			oJob.set (SchedulerPlugin.Spec.Job.Id, oJob.get (SchedulerPlugin.Spec.DatabaseJob.Job));
			oJob.remove (SchedulerPlugin.Spec.DatabaseJob.Job);
		}

		String 		id 			= Json.getString (oJob, Spec.Job.Id);
		String 		expression	= Json.getString (oJob, Spec.Job.Expression);
		int 		timeInSecs	= Json.getInteger (oJob, Spec.Job.Interval, 3600);
		JsonObject 	lifecycle	= Json.getObject (oJob, Spec.Job.Lifecycle);
		boolean 	running		= Json.getBoolean (oJob, Spec.Job.Running, true);
		
		if (expression == null && timeInSecs <= 0) {
			throw new SchedulerException ("Job requires an expression or Time in Seconds to be scheduled");
		}
		
		Date now		= new Date ();
		Date startTime 	= Json.getDate (oJob, SchedulerPlugin.Spec.Job.StartTime);
		Date endTime 	= Json.getDate (oJob, SchedulerPlugin.Spec.Job.EndTime);
		
		if (endTime != null && endTime.before (now)) {
			return SchedulingResult.Expired;
		}
		
		if (endTime != null) {
			Date starting = startTime;
			if (starting == null) {
				starting = now;
			}
			if (endTime.before (starting)) {
				return SchedulingResult.Expired;
			}
		}

		space.tracer ().log (Level.Info, "Schedule Job {0}", id);
		JobBuilder builder = newJob (JobTask.class).withIdentity (Prefix.Job + id, this.id);
		
		builder.usingJobData (SchedulerPlugin.Spec.Job.Id, id);
		builder.usingJobData (SchedulerPlugin.Spec.Job.Space, space.getNamespace ());
		builder.usingJobData (SchedulerPlugin.Spec.Job.Lifecycle, lifecycle.toString (0, true));
		
		JobDetail job = builder.build ();
		
		String timeZone = Json.getString (this.spec, SchedulerPlugin.Spec.TimeZone);
		
		TriggerBuilder<Trigger> triggerBuilder = newTrigger ()
				.withIdentity (Prefix.Trigger + id, this.id);
		
		if (expression != null) {
			space.tracer ().log (Level.Info, "Schedule Job {0} with expression {1}", id, expression);
			CronScheduleBuilder csb = cronSchedule (expression);
			if (!Lang.isNullOrEmpty (timeZone)) {
				csb.inTimeZone (TimeZone.getTimeZone (timeZone));
			}
			triggerBuilder.withSchedule (csb);
		} else if (timeInSecs > 0) {
			space.tracer ().log (Level.Info, "Schedule Job {0} with timeInSecs {1}", id, timeInSecs);
			triggerBuilder.withSchedule (simpleSchedule ().withIntervalInSeconds (timeInSecs).repeatForever ());
		}
		
		if (startTime != null) {
			space.tracer ().log (Level.Info, "  w/ StartTime {0}", Lang.toUTC (startTime));
			triggerBuilder.startAt (startTime);
		}
		if (endTime != null) {
			space.tracer ().log (Level.Info, "  w/ EndTime {0}", Lang.toUTC (endTime));
			triggerBuilder.endAt (endTime);
		}
		
		oScheduler.scheduleJob (job, triggerBuilder.build ());
		if (!running) {
			oScheduler.pauseJob (jobKey (Prefix.Job + id, this.id));
		}
		return SchedulingResult.Scheduled;
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

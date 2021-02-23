package com.bluenimble.platform.scheduler.impls.quartz;

import java.io.IOException;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiSpace.Features;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.impls.PluginClassLoader;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.server.plugins.scheduler.SchedulerPlugin;
import com.bluenimble.platform.shell.Shell;

public class JobTask implements Job {
	
	public interface Spec {
		String FireTime 				= "fireTime";
		String PreviousFireTime 		= "previousFireTime";
		String NextFireTime 			= "nextFireTime";
		String ScheduledFireTime 		= "scheduledFireTime";
		String RunTime 					= "runTime";
		String Action 					= "action";
	}
	
	enum ActionType {
		shell,
		remote
	}
	
	private String action = SchedulerPlugin.Spec.Lifecycle.Action.Tick;
	
	public JobTask () {
		
	}
	
	public JobTask (String action) {
		this.action = action;
	}
	
	public void execute (JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap data = context.getJobDetail ().getJobDataMap ();

		PluginClassLoader pcl = (PluginClassLoader)JobTask.class.getClassLoader ();
		ApiSpace space = ((SchedulerPlugin)pcl.getPlugin ()).server ().space (data.getString (SchedulerPlugin.Spec.Job.Space));
		
		space.tracer ().log (Level.Info, "Execute Job {0}", context.getJobDetail ().getKey ());
		
		JsonObject lifecycle;
		try {
			lifecycle = new JsonObject ((String)data.get (SchedulerPlugin.Spec.Job.Lifecycle));
		} catch (JsonException e) {
			throw new JobExecutionException (e.getMessage (), e);
		}
		
		try {
			this.process (space, lifecycle, jobRuntime (context));
		} catch (Exception e) {
			throw new JobExecutionException (e.getMessage (), e);
		}
		
	}
	
	public void process (ApiSpace space, JsonObject lifecycle, JsonObject runtime) {
		JsonObject oAction = Json.getObject (lifecycle, action);
		
		if (oAction == null) {
			throw new RuntimeException ("Job lifecycle " + action + " action not defined");
		}
		
		ActionType serviceType = ActionType.shell;
		try {
			serviceType = ActionType.valueOf (
				Json.getString (oAction, SchedulerPlugin.Spec.Lifecycle.Type, ActionType.shell.name ())
			);
		} catch (Exception e) {
			// Ignore, default to shell
		}
		
		switch (serviceType) {
			case shell:
				shell (space, runtime, oAction);
				break;
			case remote:
				remote (space, runtime, oAction);
				break;
			default:
				break;
		}
	}
	
	private void remote (ApiSpace space, JsonObject runtime, JsonObject oAction) {
		Remote oRemote = space.feature (
			Remote.class, 
			Json.getString (oAction, SchedulerPlugin.Spec.Lifecycle.Feature, Features.Default), 
			ApiContext.Instance
		);
		
		JsonObject request = Json.getObject (oAction, SchedulerPlugin.Spec.Lifecycle.Request).duplicate ();
		
		JsonObject rData = Json.getObject (request, Remote.Spec.Data);
		if (rData == null) {
			rData = new JsonObject ();
			request.set (Remote.Spec.Data, rData);
		}
		
		rData.set (SchedulerPlugin.Spec.Lifecycle.Runtime, runtime);
		
		space.tracer ().log (Level.Info, "Execute Remote Job Request {0}", request);
		
		oRemote.post (
			request,
			new Remote.Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onError (int status, Object message) throws IOException {
					space.tracer ().log (Level.Info, ">>>> Remote Job Error {0} - {1}", status, message);
				}
				@Override
				public void onDone (int status, Object data) throws IOException {
					space.tracer ().log (Level.Info, ">>>> Remote Job Success {0} - {1}", status, data);
				}
				@Override
				public void onData (int status, byte[] chunk) throws IOException {
				}
			},
			(ApiStreamSource [])null
		);
	}
	
	private void shell (ApiSpace space, JsonObject runtime, JsonObject oAction) {
		Shell oShell = space.feature (
			Shell.class, 
			Json.getString (oAction, SchedulerPlugin.Spec.Lifecycle.Feature, Features.Default), 
			ApiContext.Instance
		);
		
		space.tracer ().log (Level.Info, "Execute Shell Job {0}", oAction);
		
		JsonObject params = oAction.duplicate ();
		params.set (SchedulerPlugin.Spec.Lifecycle.Runtime, runtime);
		
		JsonObject result = oShell.run (Json.getString (oAction, SchedulerPlugin.Spec.Lifecycle.Command), params);
		
		space.tracer ().log (Level.Info, ">>>> Shell Job Result {0}", result);
	}
	
	private JsonObject jobRuntime (JobExecutionContext context) {
		JsonObject runtime = new JsonObject ();
		
		runtime.set (Spec.Action, action);
		runtime.set (Spec.FireTime, context.getFireTime ());
		runtime.set (Spec.NextFireTime, context.getNextFireTime());
		runtime.set (Spec.PreviousFireTime, context.getPreviousFireTime ());
		runtime.set (Spec.ScheduledFireTime, context.getScheduledFireTime ());
		runtime.set (Spec.RunTime, context.getJobRunTime ());
		
		return runtime;
	}
	
}

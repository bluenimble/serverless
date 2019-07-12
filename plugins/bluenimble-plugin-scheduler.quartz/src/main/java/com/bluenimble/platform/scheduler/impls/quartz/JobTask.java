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
	
	interface Spec {
		String FireTime 				= "fireTime";
		String PreviousFireTime 		= "previousfireTime";
		String NextFireTime 			= "nextfireTime";
		String ScheduledFireTime 		= "scheduledFireTime";
		String RunTime 					= "runTime";
	}
	
	enum ServiceType {
		shell,
		remote
	}
	
	public void execute (JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap data = context.getJobDetail ().getJobDataMap ();

		PluginClassLoader pcl = (PluginClassLoader)JobTask.class.getClassLoader ();
		ApiSpace space = ((SchedulerPlugin)pcl.getPlugin ()).server ().space (data.getString (SchedulerPlugin.Spec.Job.Space));
		
		space.tracer ().log (Level.Info, "Execute Job {0}", context.getJobDetail ().getKey ());
		
		JsonObject service;
		try {
			service = new JsonObject ((String)data.get (SchedulerPlugin.Spec.Job.Service));
		} catch (JsonException e) {
			throw new JobExecutionException (e.getMessage (), e);
		}
		
		ServiceType serviceType = ServiceType.shell;
		try {
			serviceType = ServiceType.valueOf (
				Json.getString (service, SchedulerPlugin.Spec.Service.Type, ServiceType.shell.name ())
			);
		} catch (Exception e) {
			// Ignore, default to shell
		}
		
		switch (serviceType) {
			case shell:
				shell (space, context, service);
				break;
			case remote:
				remote (space, context, service);
				break;
			default:
				break;
		}
		
	}
	
	private void remote (ApiSpace space, JobExecutionContext context, JsonObject service) {
		Remote oRemote = space.feature (
			Remote.class, 
			Json.getString (service, SchedulerPlugin.Spec.Service.Feature, Features.Default), 
			ApiContext.Instance
		);
		
		JsonObject request = Json.getObject (service, SchedulerPlugin.Spec.Service.Request).duplicate ();
		
		JsonObject rData = Json.getObject (request, Remote.Spec.Data);
		if (rData == null) {
			rData = new JsonObject ();
			request.set (Remote.Spec.Data, rData);
		}
		
		rData.set (SchedulerPlugin.Spec.Service.Runtime, jobRuntime (context));
		
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
	
	private void shell (ApiSpace space, JobExecutionContext context, JsonObject service) {
		Shell oShell = space.feature (
			Shell.class, 
			Json.getString (service, SchedulerPlugin.Spec.Service.Feature, Features.Default), 
			ApiContext.Instance
		);
		
		space.tracer ().log (Level.Info, "Execute Shell Job {0}", service);
		
		JsonObject params = service.duplicate ();
		params.set (SchedulerPlugin.Spec.Service.Runtime, jobRuntime (context));
		
		JsonObject result = oShell.run (Json.getString (service, SchedulerPlugin.Spec.Service.Command), params);
		
		space.tracer ().log (Level.Info, ">>>> Shell Job Result {0}", result);
	}
	
	private JsonObject jobRuntime (JobExecutionContext context) {
		JsonObject runtime = new JsonObject ();
		
		runtime.set (Spec.FireTime, context.getFireTime ());
		runtime.set (Spec.NextFireTime, context.getJobRunTime ());
		runtime.set (Spec.PreviousFireTime, context.getPreviousFireTime ());
		runtime.set (Spec.ScheduledFireTime, context.getScheduledFireTime ());
		runtime.set (Spec.RunTime, context.getJobRunTime ());
		
		return runtime;
	}
	
}

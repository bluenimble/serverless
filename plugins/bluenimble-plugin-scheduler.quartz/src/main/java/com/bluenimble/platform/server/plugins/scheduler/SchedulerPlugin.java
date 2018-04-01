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

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.CodeExecutor;
import com.bluenimble.platform.api.impls.scheduler.SchedulerApiRequest;
import com.bluenimble.platform.api.impls.scheduler.SchedulerApiResponse;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;

public class SchedulerPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	public static final String _Space 		= "__INTERNAL__SPACE__";
	public static final String _Api 		= "__INTERNAL__API__";
	public static final String _Service 	= "__INTERNAL__SERVICE__";
		
	interface Prefix {
		String Scheduler 	= "scheduler-";
		String Group 		= "grp-";
		String Job 			= "job-";
		String Trigger 		= "trg-";
	}
	
	interface Spec {
		String Expression 		= "expression";
		String TimeZone 		= "timeZone";
		String Data 			= "data";
		String Api 				= "api";
		String Service 			= "service";
	}
	
	private Scheduler 	oScheduler;
	
	private JsonObject 	scheduler;
	
	private String 	feature = "scheduler";
	
	private ApiServer server;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		this.server = server;
		
		Properties props = new Properties ();
		Iterator<String> keys = scheduler.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			props.put (key, scheduler.get (key));
		}
		
		StdSchedulerFactory factory = new StdSchedulerFactory ();
		factory.initialize (props);
		
		oScheduler = factory.getScheduler ();
	
		oScheduler.start ();
		
	}
	
	@Override
	public void onEvent (Event event, Object target) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Create:
				startJobs  (space);
				break;
			case AddFeature:
				startJobs (space);
				break;
			case DeleteFeature:
				stopJobs (space);
				break;
			default:
				break;
		}
	}
	
	private void startJobs (ApiSpace space) throws PluginRegistryException {
		JsonObject schedulerFeature = Json.getObject (space.getFeatures (), feature);
		if (schedulerFeature == null || schedulerFeature.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = schedulerFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject source = Json.getObject (schedulerFeature, key);
			
			if (!this.getName ().equalsIgnoreCase (Json.getString (source, ApiSpace.Features.Provider))) {
				continue;
			}
			
			JsonObject spec = Json.getObject (source, ApiSpace.Features.Spec);
			if (spec == null) {
				continue;
			}
			
			String expression = Json.getString (source, Spec.Expression);
			if (Lang.isNullOrEmpty (expression)) {
				continue;
			}
			
			String api = Json.getString (source, Spec.Api);
			if (Lang.isNullOrEmpty (api)) {
				continue;
			}
			
			String service = Json.getString (source, Spec.Service);
			if (Lang.isNullOrEmpty (service)) {
				continue;
			}
			
			String id = space.getNamespace () + Lang.DASH + key;
			
			JobBuilder builder = newJob (ServiceJob.class)
				      .withIdentity (Prefix.Job + id, Prefix.Group + id);

			builder.usingJobData (_Space, space.getNamespace ());
			builder.usingJobData (_Api, api);
			builder.usingJobData (_Service, service);
			
			JsonObject data = Json.getObject (spec, Spec.Data);
			if (!Json.isNullOrEmpty (data)) {
				Iterator<String> dataKeys = data.keys ();
				while (dataKeys.hasNext ()) {
					String dataKey = dataKeys.next ();
					builder.usingJobData (dataKey, String.valueOf (data.get (dataKey)));
				}
			}
			
			JobDetail job = builder.build ();
			
			String timeZone = Json.getString (source, Spec.TimeZone);
			
			CronScheduleBuilder csb = cronSchedule (Json.getString (source, Spec.Expression));
			if (!Lang.isNullOrEmpty (timeZone)) {
				csb.inTimeZone (TimeZone.getTimeZone (timeZone));
			}
			Trigger trigger = newTrigger ()
					.withIdentity (Prefix.Trigger + id, Prefix.Group + id)
					.withSchedule (csb)
				    .forJob (job.getKey ())
				    .build ();
			
			try {
				oScheduler.scheduleJob (trigger);
			} catch (SchedulerException e) {
				throw new PluginRegistryException (e.getMessage (), e);
			}
			
		}
	}

	private void stopJobs (ApiSpace space) throws PluginRegistryException {
		JsonObject schedulerFeature = Json.getObject (space.getFeatures (), feature);
		if (schedulerFeature == null || schedulerFeature.isEmpty ()) {
			return;
		}
		
		Iterator<String> keys = schedulerFeature.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject source = Json.getObject (schedulerFeature, key);
			
			if (!this.getName ().equalsIgnoreCase (Json.getString (source, ApiSpace.Features.Provider))) {
				continue;
			}
			
			JsonObject spec = Json.getObject (source, ApiSpace.Features.Spec);
			if (spec == null) {
				continue;
			}
			
			String id = space.getNamespace () + Lang.DASH + key;
			
			try {
				oScheduler.deleteJob (jobKey (Prefix.Job + id, Prefix.Group + id));
			} catch (SchedulerException e) {
				throw new PluginRegistryException (e.getMessage (), e);
			}
			
		}
	}

	@Override
	public void kill () {
		if (oScheduler == null) {
			return;
		}
		try {
			oScheduler.shutdown (true);
		} catch (Throwable e) {
			// IGNORE
		}
		
		super.kill ();
		
	}
	
	public class ServiceJob implements Job {
		
		public void execute (JobExecutionContext context) throws JobExecutionException {
			
			JobDataMap data = context.getJobDetail ().getJobDataMap ();
			
			// create request / response and make the call
			ApiRequest request = new SchedulerApiRequest (server, context, new ApiSpace.Endpoint () {
				@Override
				public ApiVerb verb () {
					return ApiVerb.POST;
				}
				
				@Override
				public String space () {
					return data.getString (_Space);
				}
				
				@Override
				public String api () {
					return data.getString (_Api);
				}
				
				@Override
				public String [] resource () {
					String sService = data.getString (_Service);
					if (sService.startsWith (Lang.SLASH)) {
						sService.substring (1);
					}
					return Lang.split (sService, Lang.SLASH);
				}
			});
			server.execute (request, new SchedulerApiResponse (request.getId ()), CodeExecutor.Mode.Sync);
			
		}
		
	}
	
	public JsonObject getScheduler() {
		return scheduler;
	}

	public void setScheduler(JsonObject scheduler) {
		this.scheduler = scheduler;
	}

}

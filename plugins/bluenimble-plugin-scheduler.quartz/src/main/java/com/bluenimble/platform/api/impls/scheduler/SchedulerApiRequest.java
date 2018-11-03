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
package com.bluenimble.platform.api.impls.scheduler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.AbstractApiRequest;
import com.bluenimble.platform.iterators.EmptyIterator;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.Scriptable;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.plugins.scheduler.SchedulerPlugin;

@Scriptable (name = "ApiRequest")
public class SchedulerApiRequest extends AbstractApiRequest {

	private static final long serialVersionUID 					= -7827772016083090070L;
	
	// private static final Logger 				logger 			= Logger.getLogger (SchedulerApiRequest.class.getName ());
	
	private static final String 				Channel			= "cron";
	private static final String 				Origin 			= "Cron";

	private static final String 				Scheme 			= "baas";
	
	interface Spec {
		
		String Data 					= "data";
		
		String FireTime 				= "fireTime";
		String PreviousFireTime 		= "PreviousfireTime";
		String NextFireTime 			= "nextfireTime";
		String ScheduledFireTime 		= "scheduledFireTime";
		String RunTime 					= "runTime";
		
	}
	
	private static final EmptyIterator<String> 	EmptyIterator	= new EmptyIterator<String> ();
	
	private String endpoint;
	private String path;
	
	private Map<String, Object> 			headers;
	
	public SchedulerApiRequest (ApiServer server, JobExecutionContext context, ApiSpace.Endpoint endpoint) {
		
		super ();
		
		this.space 			= endpoint.space ();
		this.channel 		= Channel;
		
		this.api 			= endpoint.api ();
		this.verb 			= endpoint.verb ();
		this.endpoint 		= Lang.SLASH + context.getJobDetail ().getKey ();
		
		this.resource		= endpoint.resource ();
		if (resource == null || resource.length == 0) {
			this.resource 	= new String [] { Lang.SLASH };
			this.path 		= Lang.SLASH;
		} else {
			this.path 		= Lang.SLASH + Lang.join (resource, Lang.SLASH);
		}
		
		node = new JsonObject ();
		
		this.device 		= (JsonObject)new JsonObject ()
								.set (ApiRequest.Fields.Device.Origin, Origin)
								.set (ApiRequest.Fields.Device.Language, Locale.ENGLISH);

		// set payload
		JsonObject payload = new JsonObject ();
		
		payload.set (Spec.FireTime, context.getFireTime ());
		payload.set (Spec.NextFireTime, context.getJobRunTime ());
		payload.set (Spec.PreviousFireTime, context.getPreviousFireTime ());
		payload.set (Spec.ScheduledFireTime, context.getScheduledFireTime ());
		payload.set (Spec.RunTime, context.getJobRunTime ());
		
		JobDetail detail = context.getJobDetail ();
		
		JobDataMap data = detail.getJobDataMap ();
		
		if (data != null && !data.isEmpty ()) {
			JsonObject oData = new JsonObject ();
			payload.set (Spec.Data, oData);
			Iterator<String> dataKeys = data.keySet ().iterator ();
			while (dataKeys.hasNext ()) {
				String key = dataKeys.next ();
				if (key.equals (SchedulerPlugin._Space) || key.equals (SchedulerPlugin._Api) || key.equals (SchedulerPlugin._Service)) {
					continue;
				}
				oData.put (dataKeys, data.get (key));
			}
		}
		
		// context.
		
		// payload.set (name, value)
		
		set (Payload, payload, Scope.Parameter);
		
	}

	@Override
	public String getEndpoint () {
		return endpoint;
	}

	@Override
	public String getLang () {
		return parent.getLang ();
	}

	@Override
	public String getScheme () {
		return Scheme;
	}

	@Override
	public Iterator<String> keys (Scope scope) {
		switch (scope) {
			case Header:
				if (headers == null || headers.isEmpty ()) {
					return EmptyIterator;
				}
				return headers.keySet ().iterator ();
			case Parameter:
				if (application == null) {
					return EmptyIterator;
				} 
				return application.keySet ().iterator ();
			default:
				break;
		}
		return EmptyIterator;
	}
	
	@Override
	public void forEach (Scope scope, ForEachCallback callback) {
		Iterator<String> keys = null;
		switch (scope) {
			case Header:
				if (headers != null && !headers.isEmpty ()) {
					keys = headers.keySet ().iterator ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						callback.visit (key, headers.get (key));
					}
				}
				break;
			case Parameter:
				if (application != null && !application.isEmpty ()) {
					keys = application.keySet ().iterator ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						callback.visit (key, application.get (key));
					}
				}
				break;
			default:
				break;
		}
	}

	@Override
	protected void setHeader (String name, Object value) {
		if (headers == null) {
			headers = new HashMap<String, Object> ();
		}
		headers.put (name, value);
	}

	@Override
	protected Object getByScope (String name, Scope scope) {
		switch (scope) {
			case Header:
				if (headers == null) {
					return null;
				}
				return headers.get (name);
			case Parameter:
				if (application == null) {
					return null;
				}
				return application.get (name);
			default:
				break;
		}
		return null;
	}
	
	@Override
	public void destroy () {
		
		super.destroy ();
		
		if (headers != null) {
			headers.clear ();
			headers = null;
		}
	}

	@Override
	public String getPath () {
		return path;
	}
}

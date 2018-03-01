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
package com.bluenimble.platform.server.tracking.impls.database;

import java.lang.management.ManagementFactory;
import java.util.Date;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.tracking.ServerRequestTrack;
import com.sun.management.ThreadMXBean;

@SuppressWarnings("restriction")
public class DatabaseApiRequestTrack implements ServerRequestTrack {

	private static final long serialVersionUID = 183213137772163278L;
	
	private static final ThreadMXBean 	MX = (ThreadMXBean)ManagementFactory.getThreadMXBean ();
	
	private static final String 		DefaultEntity 	= "ApiRequests";

	interface Spec {
		String Entity 	= "entity";
		String Feature 	= "feature";
	}
	
	interface Fields {
		String Service = "service";
		interface Time {
			String Received = "received";
			String Started 	= "started";
			String Finished = "finished";
		}
		interface Metrics {
			String Cpu 		= "cpu";
			String Memory 	= "memory";
		}
		interface Tag {
			String Name 	= "name";
			String Reason 	= "reason";
		}
		String Consumer = "consumer";
		String Status 	= "status";
		String Feedback = "feedback";
		
		String Custom = "custom";
	}
	
	private DatabaseApiRequestTracker 	tracker;
	private Api 						api;
	
	private JsonObject 					track;
	
	public DatabaseApiRequestTrack (DatabaseApiRequestTracker tracker, Api api, ApiRequest request) {
		
		this.tracker = tracker;
		this.api = api;
		
		Date now = new Date ();
		
		JsonObject oRequest = request.toJson ();
		
		track = new JsonObject ();
		track.set (ApiRequest.Fields.Id, 		request.getId ());
		track.set (ApiRequest.Fields.Channel, 	request.getChannel ());
		track.set (ApiRequest.Fields.Api, 		request.getApi ());

		// device
		String deviceKey = ApiRequest.Fields.Device.class.getSimpleName ();
		track.set (deviceKey,					Json.getObject (oRequest, deviceKey));
		
		// service
		JsonObject service = new JsonObject ();
		service.set (ApiRequest.Fields.Verb, 	request.getVerb ().name ());
		service.set (ApiRequest.Fields.Path, 	request.getPath ());
		track.set (Fields.Service, service);
		
		// request data
		String dataKey = ApiRequest.Fields.Data.class.getSimpleName ();
		track.set (dataKey,						Json.getObject (oRequest, dataKey));
		
		// request time
		JsonObject time = new JsonObject ();
		time.set (Fields.Time.Received, request.getTimestamp ());
		time.set (Fields.Time.Started, now);
		
		track.set (Fields.Time.class.getSimpleName ().toLowerCase (), time);
		
		track.set (Fields.Metrics.class.getSimpleName ().toLowerCase (), new JsonObject ());
		
	}

	@Override
	public void update (ApiService service) {
		Json.getObject (track, Fields.Service).set (ApiRequest.Fields.Endpoint, service.getEndpoint ());
	}

	@Override
	public void update (ApiConsumer consumer) {
		if (consumer == null) {
			return;
		}
		JsonObject oConsumer = new JsonObject ();
		oConsumer.set (ApiConsumer.Fields.Type, consumer.get (ApiConsumer.Fields.Type));
		oConsumer.set (ApiConsumer.Fields.Id, consumer.get (ApiConsumer.Fields.Id));
		oConsumer.set (ApiConsumer.Fields.Token, consumer.get (ApiConsumer.Fields.Token));
		oConsumer.set (ApiConsumer.Fields.AccessKey, consumer.get (ApiConsumer.Fields.AccessKey));
		
		track.set (Fields.Consumer, oConsumer);

	}

	@Override
	public void finish (JsonObject feedback) {
		final Date now = new Date ();
		
		Json.getObject (track, Fields.Time.class.getSimpleName ().toLowerCase ()).set (Fields.Time.Finished, now);
		
		int code = Json.getInteger (feedback, ApiResponse.Error.Code, ApiResponse.OK.getCode ());
		
		boolean isError = code >= ApiResponse.BAD_REQUEST.getCode ();
		
		track.set (Fields.Status, isError ? 0 : 1);
		
		track.set (Fields.Feedback, feedback);
		
	    JsonObject metrics = Json.getObject (track, Fields.Metrics.class.getSimpleName ().toLowerCase ());
	    
	    if (MX.isCurrentThreadCpuTimeSupported ()) {
		    metrics.set (Fields.Metrics.Cpu, MX.getCurrentThreadUserTime ());
	    }
	    if (MX.isCurrentThreadCpuTimeSupported ()) {
		    metrics.set (Fields.Metrics.Memory, MX.getThreadAllocatedBytes (Thread.currentThread ().getId ()));
	    }

	    try {
		    tracker.executor ().execute (
				new Runnable () {
					@Override
					public void run () {
					    // shrink
						track.shrink ();
						
						JsonObject oTracking = api.getTracking ();
						
						// put
						Database db = null;
						try {
							
							db = api.space ().feature (
								Database.class, 
								Json.getString (oTracking, Spec.Feature, ApiSpace.Features.Default), 
								tracker.context ()
							);
							
							if (db == null) {
								api.tracer ().log (Tracer.Level.Error, "CantStore[{0}] due to database declared in the api is not defined at the space level", track.toString ());
								return;
							}
							
							// put
							
							DatabaseObject entity = db.create (Json.getString (oTracking, Spec.Entity, DefaultEntity));
							entity.load (track);
							entity.save ();
							
						} catch (DatabaseException e) {
							api.tracer ().log (Tracer.Level.Error, "CantStore[{0}] due to {1}", track.toString (), e.getMessage ());
							api.tracer ().log (Tracer.Level.Error, Lang.BLANK, e);
						} finally {
							if (db != null) { db.recycle ();}
						}
					}
				}	
			);
		} catch (Exception ex) {
			api.tracer ().log (Tracer.Level.Error, Lang.BLANK, ex);
	    }

	}

	@Override
	public void put (String name, Object value) {
		if (Lang.isNullOrEmpty (name) || value == null) {
			return;
		}
		JsonObject custom = Json.getObject (track, Fields.Custom);
		if (custom == null) {
			custom = new JsonObject ();
			track.set (Fields.Custom, custom);
		} 
		custom.set (name, value);
	}
	
	@Override
	public void tag (String name, String reason) {
		if (Lang.isNullOrEmpty (name)) {
			return;
		}
		JsonObject tag = Json.getObject (track, Fields.Tag.class.getSimpleName ().toLowerCase ());
		if (tag == null) {
			tag = new JsonObject ();
			track.set (Fields.Tag.class.getSimpleName ().toLowerCase (), tag);
		} 
		tag.set (Fields.Tag.Name, name);
		tag.set (Fields.Tag.Reason, reason);
	}

}

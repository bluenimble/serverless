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
package com.bluenimble.platform.server.tracking.impls.indexer;

import java.lang.management.ManagementFactory;
import java.util.Date;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Fields.Node;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.indexer.Indexer;
import com.bluenimble.platform.indexer.IndexerException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.tracking.ServerRequestTrack;
import com.sun.management.ThreadMXBean;

@SuppressWarnings("restriction")
public class IndexerApiRequestTrack implements ServerRequestTrack {

	private static final long serialVersionUID = 183213137772163278L;
	
	private static final String 		DateFormat 	= "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	private static final ThreadMXBean 	MX = (ThreadMXBean)ManagementFactory.getThreadMXBean ();

	interface Spec {
		String Entity 	= "entity";
		String Feature 	= "feature";
	}
	
	private static final String 		DefaultEntity 	= "requests";

	interface Fields {
		interface Service {
			String Name = "name";
		}
		interface Time {
			String Received = "received";
			String Started 	= "started";
			String Finished = "finished";
		}
		interface Metrics {
			String Cpu 		= "cpu";
			String Memory 	= "memory";
		}
		interface Api {
			String Namespace 	= "ns";
			String Name 		= "name";
		}
		interface Tag {
			String Name 	= "name";
			String Reason 	= "reason";
		}
		interface FeedBack {
			String Message 	= "message";
		}
		String Consumer = "consumer";
		String Status 	= "status";
		String Feedback = "feedback";
		
		String Custom = "custom";
	}
	
	interface Elk {
		String Mapping 		= "mapping";
		String Properties 	= "properties";
		String Fields 		= "fields";
		String Keyword 		= "keyword";
		
		interface Type {
			String Text 	= "text";
			String Integer 	= "integer";
			String Double 	= "double";
			String Keyword 	= "keyword";
		}
	}
	
	private IndexerApiRequestTracker 	tracker;
	private Api 						api;
	
	private JsonObject 					track;
	
	public IndexerApiRequestTrack (IndexerApiRequestTracker tracker, Api api, ApiRequest request) {
		
		this.tracker = tracker;
		this.api = api;
		
		Date now = new Date ();
		
		JsonObject oRequest = request.toJson ();
		
		track = new JsonObject ();
		track.set (ApiRequest.Fields.Id, 		request.getId ());
		track.set (ApiRequest.Fields.Channel, 	request.getChannel ());
		track.set (ApiRequest.Fields.Api, 		new JsonObject ()
				.set (Fields.Api.Namespace, api.getNamespace ())
				.set (Fields.Api.Name, api.getName ()));

		// device
		String deviceKey = ApiRequest.Fields.Device.class.getSimpleName ();
		track.set (deviceKey, Json.getObject (oRequest, deviceKey));
		
		// service
		JsonObject service = new JsonObject ();
		service.set (ApiRequest.Fields.Verb, 	request.getVerb ().name ());
		service.set (ApiRequest.Fields.Path, 	request.getPath ());
		track.set (Fields.Service.class.getSimpleName ().toLowerCase (), service);
		
		// request data
		String dataKey = ApiRequest.Fields.Data.class.getSimpleName ().toLowerCase ();
		track.set (dataKey,	Json.getObject (oRequest, dataKey));
		
		// request time
		JsonObject time = new JsonObject ();
		time.set (Fields.Time.Received, Lang.toString (request.getTimestamp (), DateFormat));
		time.set (Fields.Time.Started, Lang.toString (now, DateFormat));
		
		track.set (Fields.Time.class.getSimpleName ().toLowerCase (), time);
		
		track.set (Fields.Metrics.class.getSimpleName ().toLowerCase (), new JsonObject ());
		
	}

	@Override
	public void update (ApiService service) {
		Json.getObject (track, Fields.Service.class.getSimpleName ().toLowerCase ())
			.set (Fields.Service.Name, 			service.getName ())
			.set (ApiRequest.Fields.Endpoint, 	service.getEndpoint ());
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
		
		Json.getObject (track, Fields.Time.class.getSimpleName ().toLowerCase ()).set (Fields.Time.Finished, Lang.toString (now, DateFormat));
		
		int code = Json.getInteger (feedback, ApiResponse.Error.Code, ApiResponse.OK.getCode ());
		
		track.set (Fields.Status, code);
		
		if (feedback.containsKey (Fields.FeedBack.Message)) {
			feedback.set (Fields.FeedBack.Message, feedback.get (Fields.FeedBack.Message).toString ());
		}
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
					    api.tracer ().log (Tracer.Level.Info, "Executor Started Track Thread ...");
					    // shrink
						track.shrink ();
					
						JsonObject oTracking = api.getTracking ();
						
						// put
						Indexer indexer = null;
						try {
							
							indexer = api.space ().feature (
								Indexer.class, 
								Json.getString (oTracking, Spec.Feature, ApiSpace.Features.Default), 
								tracker.context ()
							);
							
							if (indexer == null) {
							    api.tracer ().log (Tracer.Level.Error, "CantStore[{0}] due to indexer declared in the api is not defined at the space level", track.toString ());
								return;
							}
							
							String entity = Json.getString (oTracking, Spec.Entity, DefaultEntity);
							
							if (!indexer.exists (entity)) {
							    api.tracer ().log (Tracer.Level.Info, "Entity not found in Index, Create Mapping...");
								indexer.create (entity, requestMapping ());
							}
							
							indexer.put (entity, track);
							
						} catch (IndexerException e) {
						    api.tracer ().log (Tracer.Level.Error, "CantStore[{0}] due to {1}", track.toString (), e.getMessage ());
						    api.tracer ().log (Tracer.Level.Error, Lang.BLANK, e);
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
	
	private static JsonObject requestMapping () {
		JsonObject oProperties 	= new JsonObject ();
		
		String type 			= Elk.Type.class.getSimpleName ().toLowerCase ();
		JsonObject aggKeyword 	= (JsonObject) new JsonObject ().set (Elk.Keyword, new JsonObject ()
			.set (type, Elk.Type.Keyword)
		);
		
		oProperties.put (ApiRequest.Fields.Api + Lang.DOT + Fields.Api.Namespace, new JsonObject ()
			.set (type, Elk.Type.Text)
			.set (Elk.Fields, aggKeyword)
		);
		
		String serviceKey = Fields.Service.class.getSimpleName ().toLowerCase ();
		
		oProperties.put (Fields.Feedback + Lang.DOT + ApiResponse.Error.Code, new JsonObject ()
			.set (type, Elk.Type.Integer)
		);
		
		oProperties.put (Fields.Metrics.class.getSimpleName ().toLowerCase () + Lang.DOT + Fields.Metrics.Cpu, new JsonObject ()
			.set (type, Elk.Type.Double)
		);
		
		oProperties.put (Fields.Metrics.class.getSimpleName ().toLowerCase () + Lang.DOT + Fields.Metrics.Memory, new JsonObject ()
			.set (type, Elk.Type.Double)
		);
		
		oProperties.put (serviceKey + Lang.DOT + Fields.Service.Name, new JsonObject ()
			.set (type, Elk.Type.Text)
			.set (Elk.Fields, aggKeyword)
		);
			
		oProperties.put (serviceKey + Lang.DOT + ApiRequest.Fields.Endpoint, new JsonObject ()
			.set (type, Elk.Type.Text)
			.set (Elk.Fields, aggKeyword)
		);
		
		oProperties.put (serviceKey + Lang.DOT + ApiRequest.Fields.Path, new JsonObject ()
			.set (type, Elk.Type.Text)
			.set (Elk.Fields, aggKeyword)
		);
		
		oProperties.put (serviceKey + Lang.DOT + ApiRequest.Fields.Verb, new JsonObject ()
			.set (type, Elk.Type.Text)
			.set (Elk.Fields, aggKeyword)
		);
		
		oProperties.put (Node.class.getSimpleName ().toLowerCase () + Lang.DOT + Node.Type, new JsonObject ()
			.set (type, Elk.Type.Text)
			.set (Elk.Fields, aggKeyword)
		);
			
		oProperties.put (Fields.Tag.class.getSimpleName ().toLowerCase () + Lang.DOT + Fields.Tag.Name, new JsonObject ()
			.set (type, Elk.Type.Text)
			.set (Elk.Fields, aggKeyword)
		);
		
		//return (JsonObject) new JsonObject ().set (Elk.Mapping, new JsonObject ()
		return (JsonObject) new JsonObject ().set (DefaultEntity, new JsonObject ()
			.set (Elk.Properties, oProperties)
			//)
		);
	}
}
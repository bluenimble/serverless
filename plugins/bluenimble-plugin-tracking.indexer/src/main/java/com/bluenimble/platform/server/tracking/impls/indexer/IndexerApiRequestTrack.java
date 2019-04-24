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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.indexer.Indexer;
import com.bluenimble.platform.indexer.IndexerException;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.Remote.Callback;
import com.bluenimble.platform.server.tracking.ServerRequestTrack;
import com.sun.management.ThreadMXBean;

@SuppressWarnings("restriction")
public class IndexerApiRequestTrack implements ServerRequestTrack {

	private static final long serialVersionUID = 183213137772163278L;
	
	private static final String 		DateFormat 	= "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	private static final ThreadMXBean 	MX = (ThreadMXBean)ManagementFactory.getThreadMXBean ();

	interface Spec {
		String Feature 			= "feature";
		String Entity 			= "entity";
		String Fields 			= "fields";
		String LocationService 	= "locationService";
	}
	
	interface SpecialTags {
		String Feature 	= "__feature";
	}
	private static final Set<String> SpecialTagsSet = new HashSet<String> ();
	static {
		SpecialTagsSet.add (SpecialTags.Feature);
	}
	
	private static final String 		DefaultEntity 	= "all";

	interface Fields {
		interface Service {
			String Name = "name";
			String Id 	= "id";
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
		String Location = "location";
		String Status 	= "status";
		String Feedback = "feedback";
		String Referer	= "referer";
		
		String Custom = "custom";
	}
	
	private static final Set<String> DefaultFields = new HashSet<String> ();
	static {
		DefaultFields.add (Fields.Consumer);
		DefaultFields.add (Fields.Service.class.getSimpleName ().toLowerCase ());
		DefaultFields.add (Fields.Custom);
		DefaultFields.add (Fields.Tag.class.getSimpleName ().toLowerCase ());
		DefaultFields.add (Fields.Metrics.class.getSimpleName ().toLowerCase ());
	}
	
	private IndexerApiRequestTracker 	tracker;
	private Api 						api;
	
	private JsonObject 					track;
	
	private boolean 					discarded;
	private String						feature;
	private JsonObject					locationService;
	private Set<String>					fields 	= DefaultFields;
	
	public IndexerApiRequestTrack (IndexerApiRequestTracker tracker, Api api, ApiRequest request) {
		
		this.tracker = tracker;
		this.api = api;
		
		JsonArray apiTrackFields = Json.getArray (api.getTracking (), Spec.Fields);
		if (!Json.isNullOrEmpty (apiTrackFields)) {
			fields = new HashSet<String> ();
			for (int i = 0; i < apiTrackFields.count (); i++) {
				fields.add (String.valueOf (apiTrackFields.get (i)));
			}
		}
		
		locationService = Json.getObject (api.getTracking (), Spec.LocationService);
		
		Date now = new Date ();
		
		JsonObject oRequest = request.toJson ();
		
		track = new JsonObject ();
		track.set (ApiRequest.Fields.Id, 		request.getId ());
		track.set (ApiRequest.Fields.Channel, 	request.getChannel ());
		track.set (ApiRequest.Fields.Api, 		new JsonObject ()
			 .set (Fields.Api.Namespace, api.getNamespace ())
			 .set (Fields.Api.Name, api.getName ()));

		// device
		String deviceKey = ApiRequest.Fields.Device.class.getSimpleName ().toLowerCase ();
		track.set (deviceKey, Json.getObject (oRequest, deviceKey));
		
		// service
		JsonObject service = new JsonObject ();
		service.set (ApiRequest.Fields.Verb, 	request.getVerb ().name ());
		service.set (ApiRequest.Fields.Path, 	request.getPath ());
		track.set (Fields.Service.class.getSimpleName ().toLowerCase (), service);
		
		// request data
		String dataKey = ApiRequest.Fields.Data.class.getSimpleName ().toLowerCase ();
		JsonObject data = Json.getObject (oRequest, dataKey);
		track.set (dataKey,	data);
		
		track.set (Fields.Referer, Json.find (data, ApiRequest.Fields.Data.Headers, ApiHeaders.Referer));
		
		// request time
		JsonObject time = new JsonObject ();
		time.set (Fields.Time.Received, Lang.toString (request.getTimestamp (), DateFormat));
		time.set (Fields.Time.Started, Lang.toString (now, DateFormat));
		
		track.set (Fields.Time.class.getSimpleName ().toLowerCase (), time);
		
		track.set (Fields.Metrics.class.getSimpleName ().toLowerCase (), new JsonObject ());
		
	}

	@Override
	public void update (ApiService service) {
		if (discarded) {
			return;
		}
		if (service == null) {
			return;
		}
		
		JsonObject sTracking = service.getTracking ();

		JsonArray sTrackFields = Json.getArray (sTracking, Spec.Fields);
		if (!Json.isNullOrEmpty (sTrackFields)) {
			fields = new HashSet<String> ();
			for (int i = 0; i < sTrackFields.count (); i++) {
				fields.add (String.valueOf (sTrackFields.get (i)));
			}
		}

		if (!Json.isNullOrEmpty (sTracking)) {
			feature = Json.getString (sTracking, ApiService.Spec.Tracking.Feature);
		}
		
		Json.getObject (track, Fields.Service.class.getSimpleName ().toLowerCase ())
			.set (Fields.Service.Id, 			service.getId ())
			.set (Fields.Service.Name, 			service.getName ())
			.set (ApiRequest.Fields.Endpoint, 	service.getEndpoint ());
	}

	@Override
	public void update (ApiConsumer consumer) {
		if (discarded) {
			return;
		}
		if (consumer == null) {
			return;
		}
		track.set (Fields.Consumer, consumer.toJson ());
	}

	@Override
	public void finish (JsonObject feedback) {
		
		if (discarded || fields.isEmpty ()) {
			track.clear ();
			return;
		}
		
		if (!fields.contains (Fields.Consumer)) {
			track.remove (Fields.Consumer);
		}
		if (!fields.contains (Fields.Service.class.getSimpleName ().toLowerCase ())) {
			track.remove (Fields.Service.class.getSimpleName ().toLowerCase ());
		}
		if (!fields.contains (ApiRequest.Fields.Data.class.getSimpleName ().toLowerCase ())) {
			track.remove (ApiRequest.Fields.Data.class.getSimpleName ().toLowerCase ());
		}
		if (!fields.contains (Fields.Custom)) {
			track.remove (Fields.Custom);
		}
		if (!fields.contains (Fields.Tag.class.getSimpleName ().toLowerCase ())) {
			track.remove (Fields.Tag.class.getSimpleName ().toLowerCase ());
		}
		JsonObject location = 
			(JsonObject)Json.find (
				track, 
				ApiRequest.Fields.Device.class.getSimpleName ().toLowerCase (), 
				ApiRequest.Fields.Device.Origin
			);
		if (Json.isNullOrEmpty (location) && fields.contains (Fields.Location) && !Json.isNullOrEmpty (locationService)) {
			Remote remote = api.space ().feature (
				Remote.class, 
				Json.getString (locationService, Spec.Feature), 
				tracker.context ()
			);
			JsonObject spec = Json.getObject (locationService, Spec.class.getSimpleName ().toLowerCase ());
			if (spec == null) {
				spec = new JsonObject ();
			} else {
				spec = spec.duplicate ();
			}
			JsonObject data = Json.getObject (spec, Remote.Spec.Data);
			if (data == null) {
				data = new JsonObject ();
				spec.set (Remote.Spec.Data, data);
			}
			data.set (
				ApiRequest.Fields.Device.Origin, 
				Json.find (track, ApiRequest.Fields.Device.class.getSimpleName ().toLowerCase (), ApiRequest.Fields.Device.Origin)
			);
			remote.get (Json.getObject (locationService, Spec.class.getSimpleName ().toLowerCase ()), new Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				}
				@Override
				public void onData (int status, byte[] chunk) throws IOException {
				}
				@Override
				public void onDone (int status, Object data) throws IOException {
					track.set (Fields.Location, data);
				}
				@Override
				public void onError (int status, Object message) throws IOException {
					api.tracer ().log (Tracer.Level.Error, "LocationProvider Error [{0}] -> {1}", status, message);
				}
			});
		}

		final Date now = new Date ();
		
		Json.getObject (track, Fields.Time.class.getSimpleName ().toLowerCase ()).set (Fields.Time.Finished, Lang.toString (now, DateFormat));
		
		int code = Json.getInteger (feedback, ApiResponse.Error.Code, ApiResponse.OK.getCode ());
		
		track.set (Fields.Status, code);
		
		if (feedback.containsKey (Fields.FeedBack.Message)) {
			feedback.set (Fields.FeedBack.Message, feedback.get (Fields.FeedBack.Message).toString ());
		}
		track.set (Fields.Feedback, feedback);
		
		if (!fields.contains (Fields.Metrics.class.getSimpleName ().toLowerCase ())) {
			track.remove (Fields.Metrics.class.getSimpleName ().toLowerCase ());
		} else {
		    JsonObject metrics = Json.getObject (track, Fields.Metrics.class.getSimpleName ().toLowerCase ());
		    if (MX.isCurrentThreadCpuTimeSupported ()) {
			    metrics.set (Fields.Metrics.Cpu, MX.getCurrentThreadUserTime ());
		    }
		    if (MX.isCurrentThreadCpuTimeSupported ()) {
			    metrics.set (Fields.Metrics.Memory, MX.getThreadAllocatedBytes (Thread.currentThread ().getId ()));
		    }
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
							
							String feature = IndexerApiRequestTrack.this.feature != null ? 
									IndexerApiRequestTrack.this.feature : 
									Json.getString (oTracking, Spec.Feature, ApiSpace.Features.Default);
							
							indexer = api.space ().feature (
								Indexer.class, 
								feature, 
								tracker.context ()
							);
							
							if (indexer == null) {
							    api.tracer ().log (Tracer.Level.Error, "CantStore[{0}] due to indexer declared in the api is not defined at the space level", track.toString ());
								return;
							}
							
							String entity = Json.getString (oTracking, Spec.Entity, DefaultEntity);
							
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
		if (discarded) {
			return;
		}
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
	public void discard (boolean discard) {
		discarded = discard;
	}

	@Override
	public void tag (String name, String reason) {
		if (discarded) {
			return;
		}
		if (Lang.isNullOrEmpty (name)) {
			return;
		}
		if (SpecialTagsSet.contains (name.toLowerCase ())) {
			if (name.toLowerCase ().equals (SpecialTags.Feature)) {
				feature = reason;
			}
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
	/*
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
	*/
}
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
package com.bluenimble.platform.server.plugins.media.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiResponse.Status;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.media.ApiMediaProcessor;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.utils.ApiUtils;

public class MediaRoutingUtils {
	
	public static final String Charset 		= "charset";
	public static final String XLocation	= "xLocation";

	public static final String Success 		= "success";
	public static final String Error 		= "error";
	
	public static final String Status 		= "status";
	public static final String Headers 		= "headers";
	
	public static JsonObject processMedia (final ApiRequest request, ApiResponse response, Lang.VariableResolver vr, JsonObject mediaDef, Tracer tracer) {
		
		if (mediaDef == null || (!mediaDef.containsKey (Success) && !mediaDef.containsKey (Error))) {
			return null;
		}
		
		String selectProcessor = (String)request.get (ApiRequest.MediaSelector);
		if (Lang.isNullOrEmpty (selectProcessor)) {
			selectProcessor = Lang.STAR;
		}
		
		JsonObject mediaSelections = null; 
		if (response.getError () == null) {
			mediaSelections = Json.getObject (mediaDef, Success); 
		} else {
			mediaSelections = Json.getObject (mediaDef, Error);
			if (mediaSelections == null) {
				mediaSelections = Json.getObject (mediaDef, Success); 
			}
		}
		
		JsonObject media = Json.getObject (mediaSelections, selectProcessor);
		
		if (media != null) {
			
			int status = Json.getInteger (media, Status, 0);
			if (status > 0) {
				response.setStatus (new Status (status, Lang.BLANK));
			}
			
			JsonObject headers = Json.getObject (media, Headers);
			if (headers != null && !headers.isEmpty ()) {
				
				Iterator<String> names = headers.keys ();
				while (names.hasNext ()) {
					String name = names.next ();
					Object hv = headers.get (name);
					if (hv == null) {
						hv = Lang.BLANK;
					}
					if (hv instanceof JsonArray) {
						JsonArray arr = (JsonArray)hv;
						if (arr.isEmpty ()) {
							continue;
						}
						List<String> values = new ArrayList<String> ();
						for (int i = 0; i < arr.count (); i++) {
							values.add (Lang.resolve (String.valueOf (arr.get (i)), vr));
						}
						response.set (name, values);
					} else {
						hv = Lang.resolve (hv.toString (), vr);
						if (XLocation.toLowerCase ().equals (name.toLowerCase ())) {
							if (status <= 0) {
								response.setStatus (ApiResponse.MOVED_PERMANENTLY);
							}
							response.set (ApiHeaders.Location, hv);
						} else if (ApiHeaders.Location.toLowerCase ().equals (name.toLowerCase ())) {
							if (status <= 0) {
								response.setStatus (ApiResponse.MOVED_PERMANENTLY);
							}
							response.set (ApiHeaders.Location, ApiUtils.apiEndpoint (request) + hv);
						} else {
							response.set (name, hv);
						}
					}
				}
			}
			
		}

		return media;
		
	}
	
	public static JsonObject pickMedia (Api api, ApiService service, String contentType) {
		JsonObject 	mediaDef = null;
		JsonObject mediaSet = service == null ? null : service.getMedia ();
		if (mediaSet != null && !mediaSet.isEmpty ()) {
			mediaDef = Json.getObject (mediaSet, contentType);
		}
		if (mediaDef == null) {
			mediaDef = Json.getObject (mediaSet, ApiMediaProcessor.Any);
		}
		if (mediaDef == null) {
			mediaDef = Json.getObject (api.getMedia (), contentType);
		}
		if (mediaDef == null) {
			mediaDef = Json.getObject (api.getMedia (), ApiMediaProcessor.Any);
		}
		return mediaDef;
	}
	
}
 
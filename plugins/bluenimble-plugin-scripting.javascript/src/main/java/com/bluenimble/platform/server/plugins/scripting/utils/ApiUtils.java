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
package com.bluenimble.platform.server.plugins.scripting.utils;

import java.util.Iterator;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace.Endpoint;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;

public class ApiUtils {
	
	interface Spec {
		String Space 		= "space";
		String Api 			= "api";
		String Verb 		= "verb";
		String Service 		= "service";
		String Parameters 	= "parameters";
		String Headers 		= "headers";
	}

	public static ApiOutput call (final Api api, final ApiConsumer consumer, final ApiRequest pRequest, final JsonObject oRequest) throws ApiServiceExecutionException {
		ApiRequest request = api.space ().request (pRequest, consumer, new Endpoint () {
			@Override
			public String space () {
				return Json.getString (oRequest, Spec.Space, api.space ().getNamespace ());
			}
			@Override
			public String api () {
				return Json.getString (oRequest, Spec.Api, api.getNamespace ());
			}
			@Override
			public String [] resource () {
				String resource = Json.getString (oRequest, Spec.Service);
				if (resource.startsWith (Lang.SLASH)) {
					resource = resource.substring (1);
				}
				if (resource.endsWith (Lang.SLASH)) {
					resource = resource.substring (0, resource.length () - 1);
				}
				if (Lang.isNullOrEmpty (resource)) {
					return null;
				}
				return Lang.split (resource, Lang.SLASH);
			}
			@Override
			public ApiVerb verb () {
				try {
					return ApiVerb.valueOf (
						Json.getString (oRequest, Spec.Verb, ApiVerb.POST.name ()).toUpperCase ()
					);
				} catch (Exception ex) {
					return ApiVerb.POST;
				}
			}
		});
		
		JsonObject parameters = Json.getObject (oRequest, Spec.Parameters);
		if (!Json.isNullOrEmpty (parameters)) {
			Iterator<String> keys = parameters.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				request.set (key, parameters.get (key));
			}
		}
		
		JsonObject headers = Json.getObject (oRequest, Spec.Headers);
		if (!Json.isNullOrEmpty (headers)) {
			Iterator<String> keys = headers.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				request.set (key, headers.get (key), Scope.Header);
			}
		}
		
		return api.call (request);
	}
	
}

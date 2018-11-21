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
package com.bluenimble.platform.server.utils;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonObject;

public class ApiUtils {
	
	public static void logError (Api api, ApiResponse response, Tracer extraTracer) {
		JsonObject error = response.getError ();
		if (error == null) {
			return;
		}

		try {
			String s = error.toString (2, false);
			if (api != null) {
				api.tracer ().log (Level.Error, s);
			}
			if (extraTracer != null) {
				extraTracer.log (Level.Error, s);
			}
		} catch (Exception e) {
		}
	}
	
	public static void logError (ApiResponse response, Tracer extraTracer) {
		logError (null, response, extraTracer);
	}
	
	public static String apiEndpoint (ApiRequest request) {
		StringBuilder sb = new StringBuilder ();
		
		sb	.append (request.getScheme ()).append (Lang.COLON).append (Lang.SLASH).append (Lang.SLASH)
			.append (request.getEndpoint ())
			.append (request.getPath ());

		String apiEndpoint = sb.toString ();
		sb.setLength (0);
		
		return apiEndpoint;
	}
		
}

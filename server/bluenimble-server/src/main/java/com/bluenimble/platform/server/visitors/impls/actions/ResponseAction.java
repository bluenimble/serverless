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
package com.bluenimble.platform.server.visitors.impls.actions;

import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.impls.SelectiveApiRequestVisitor.Placeholder;
import com.bluenimble.platform.json.JsonObject;

public class ResponseAction implements RewriteAction {

	private static final long serialVersionUID = -2165577306745099563L;
	
	public String [] apply (ApiRequest request, Placeholder placeholder, String [] aTarget, Object value, String conditionValue) {
		
		if (value == null) {
			value = new JsonObject ().set (ApiResponse.Output.Status, 200);
		}
		
		if (!(value instanceof JsonObject)) {
			value = new JsonObject ().set (ApiResponse.Output.Status, 200).set (ApiResponse.Output.Data, value);
		}
		
		request.set (ApiRequest.Interceptors.Response, value);
		
		return aTarget;
		
	}
	
}

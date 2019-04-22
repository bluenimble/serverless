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
package com.bluenimble.platform.api.impls;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.json.JsonObject;

public abstract class BaseApiResponse implements ApiResponse {
	
	private static final long serialVersionUID = 2484293173544350202L;
	
	protected String 						id;
	protected JsonObject					node;
	protected ApiService					service;
	
	protected BaseApiResponse (String id, JsonObject node) {
		this.id 	= id;
		this.node 	= node;
	}
	
	@Override
	public String getId () {
		return id;
	}

	@Override
	public ApiService getService () {
		return service;
	}

	public void setService (ApiService service) {
		this.service = service;
	}

}

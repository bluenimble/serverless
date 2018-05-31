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
package com.bluenimble.platform.api.impls.spis;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceSpi;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.json.JsonObject;

public abstract class AbstractApiServiceSpi implements ApiServiceSpi {

	private static final long serialVersionUID = 920163706776702151L;
	
	private static final String Defaults = "defaults";
	
	@Override
	public void onStart (Api api, ApiService service, ApiContext context) throws ApiManagementException {
	}

	@Override
	public void onStop (Api api, ApiService service, ApiContext context) throws ApiManagementException {
	}
	
	protected <T> T feature (Api api, Class<T> type, String feature, ApiContext context) {
		if (Lang.isNullOrEmpty (feature)) {
			feature = ApiSpace.Features.Default;
		}
		
		JsonObject defautls = Json.getObject (api.getFeatures (), Defaults);
		if (!Json.isNullOrEmpty (defautls)) {
			feature = Json.getString (defautls, feature);
		}
		
		return api.space ().feature (type, feature, context);
	}

}

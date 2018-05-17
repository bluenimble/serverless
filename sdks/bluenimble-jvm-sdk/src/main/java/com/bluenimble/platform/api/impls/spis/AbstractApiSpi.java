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
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;

public abstract class AbstractApiSpi implements ApiSpi {

	private static final long serialVersionUID = -3548377044302415224L;

	@Override
	public void onStart 	(Api api, ApiContext context) throws ApiManagementException {
	}
	@Override
	public void onStop 		(Api api, ApiContext context) throws ApiManagementException {
	}

	@Override
	public void onRequest 	(Api api, ApiRequest request, ApiResponse response) {
	}
	@Override
	public void	onService 	(Api api, ApiService service, ApiRequest request, ApiResponse response) {
	}
	@Override
	public void	onExecute	(Api api, ApiConsumer consumer, ApiService service, ApiRequest request, ApiResponse response) {
	}
	@Override
	public void	afterExecute (Api api, ApiConsumer consumer, ApiService service, ApiRequest request, ApiResponse response) {
	}

	@Override
	public void onError 	(Api api, ApiService service, ApiConsumer consumer, ApiRequest request, ApiResponse response, JsonObject error) {
	}

	protected boolean isSecure (ApiService service) {
        return 	Json.getBoolean (service.getSecurity (), ApiService.Spec.Security.Enabled, true);
	}

}

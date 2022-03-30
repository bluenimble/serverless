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
package com.bluenimble.platform.api;

import java.io.Serializable;

import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;

public interface ApiSpi extends Serializable {

	// management life cycle
	void 		onStart 		(Api api, ApiContext context) throws ApiManagementException;
	void 		onStop 			(Api api, ApiContext context) throws ApiManagementException;

	// request life cycle
	void		onRequest		(Api api, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException;
	void		onService		(Api api, ApiService service, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException;
	void		onValidate		(Api api, ApiConsumer consumer, ApiService service, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException;
	void		onExecute		(Api api, ApiConsumer consumer, ApiService service, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException;
	void		afterExecute	(Api api, ApiConsumer consumer, ApiService service, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException;
	
	void		onError			(Api api, ApiService service, ApiConsumer consumer, ApiRequest request, ApiResponse response, JsonObject error);
	
	void 		findConsumer 	(Api api, ApiService service, ApiRequest request, ApiConsumer consumer) throws ApiAuthenticationException;

}

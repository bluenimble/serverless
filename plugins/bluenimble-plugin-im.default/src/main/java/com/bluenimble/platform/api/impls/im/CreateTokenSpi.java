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
package com.bluenimble.platform.api.impls.im;

import java.util.Date;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.im.SecurityUtils;

public class CreateTokenSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -5297356423303847595L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject payload = (JsonObject)request.get (ApiRequest.Payload);
		if (payload == null) {
			payload = new JsonObject ();
		}
		
		long age = Json.getLong (payload, ApiSpace.Spec.secrets.Age, 0);
		
		payload.merge (consumer.toJson ());
		
		payload.remove (ApiSpace.Spec.secrets.Age);
		
		payload.set (ApiConsumer.Fields.TokenType, consumer.get (ApiConsumer.Fields.TokenType));
		
		String [] tokenAndExpiration = SecurityUtils.tokenAndExpiration (api, consumer, payload, new Date (), age);
		
		JsonObject result = new JsonObject ();
		
		result.set (ApiConsumer.Fields.Id, consumer.get (ApiConsumer.Fields.Id));
		result.set (ApiConsumer.Fields.Token, tokenAndExpiration [0]);
		result.set (ApiConsumer.Fields.ExpiryDate, tokenAndExpiration [1]);
		
		return new JsonApiOutput (result);
	}

}
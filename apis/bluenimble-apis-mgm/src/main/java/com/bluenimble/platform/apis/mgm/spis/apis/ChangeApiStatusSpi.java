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
package com.bluenimble.platform.apis.mgm.spis.apis;

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiStatus;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.apis.mgm.CommonSpec;
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.json.JsonObject;

public class ChangeApiStatusSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	enum Action {
		start,
		stop,
		pause,
		resume,
		status
	}
	
	interface Output {
		String Reason = "reason";
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		String apiNs = (String)request.get (CommonSpec.Api);
		
		String sAction = (String)request.getResource () [request.getResource ().length - 1];
		
		Action action = null;
		try {
			action = Action.valueOf (sAction);
		} catch (Exception ex) {
			// ignore
		}
		if (action == null) {
			throw new ApiServiceExecutionException ("unknown change-status action " + sAction).status (ApiResponse.BAD_REQUEST);
		}
		
		ApiSpace space = null;
		
		try {
			space = MgmUtils.space (consumer, api);
			switch (action) {
				case start:
					space.start (apiNs);
					break;
				case stop:
					space.stop (apiNs);
					break;
				case pause:
					space.pause (apiNs);
					break;
				case resume:
					space.resume (apiNs);
					break;
				default:
					break;
			} 
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		Api targetApi = space.api (apiNs);
		
		JsonObject result = (JsonObject)new JsonObject ().set (Api.Spec.Status, targetApi.status ().name ());
		if (ApiStatus.Failed.equals (targetApi.status ())) {
			result.set (Output.Reason, targetApi.getFailure ());
		}
		
		return new JsonApiOutput (result);
	}

}

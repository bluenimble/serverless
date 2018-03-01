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
package com.bluenimble.platform.apis.mgm.spis.keys;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiAccessDeniedException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.apis.mgm.CommonSpec;
import com.bluenimble.platform.apis.mgm.Role;
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.security.SpaceKeyStoreException;

public class CreateKeysSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	interface Output {
		String Keys = "keys";
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		Role role = Role.DEVELOPER;
		
		String sRole = (String)request.get (CommonSpec.Role);
		if (!Lang.isNullOrEmpty (sRole)) {
			try {
				role = Role.valueOf (sRole.trim ().toUpperCase ());
			} catch (Exception ex) {
				// undefined role
			}
		}
		
		Integer pack = (Integer)request.get (CommonSpec.Pack);
		if (pack == null) {
			pack = 1;
		}
		
		Date expiryDate = (Date)request.get (CommonSpec.ExpiryDate);

		Map<String, Object> properties = new HashMap<String, Object> ();
		properties.put (CommonSpec.Role, role.name ());

		ApiSpace consumerSpace;
		try {
			consumerSpace = MgmUtils.space (consumer, api);
		} catch (ApiAccessDeniedException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.FORBIDDEN);
		}
		
		List<KeyPair> list = null;
		try {
			list = consumerSpace.keystore ().create (pack, expiryDate, properties);
		} catch (SpaceKeyStoreException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.BAD_REQUEST);
		}
		
		JsonObject result = new JsonObject ();
		JsonArray aKeys = new JsonArray ();
		result.set (Output.Keys, aKeys);
		
		if (list == null) {
			return new JsonApiOutput (result);
		}
		
		for (KeyPair skp : list) {
			aKeys.add (skp.toJson ());
		}

		return new JsonApiOutput (result);
	}

}

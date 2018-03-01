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
package com.bluenimble.platform.apis.mgm.spis.spaces;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.DescribeOption;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.apis.mgm.CommonOutput;
import com.bluenimble.platform.apis.mgm.CommonSpec;
import com.bluenimble.platform.apis.mgm.Role;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;

public class CreateSpaceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;
	
	private static final String [] SpaceModel = new String [] {"space.json"};
	
	interface SecretsDefaults {
		String 	Algorithm 	= "AES";
		int 	Age 		= 60;
	}

	interface Spec {
		String Space = "space";
	}
	
	private JsonObject	spaceModel;

	@Override
	public void onStart (Api api, ApiService service, ApiContext context) throws ApiManagementException {
		super.onStart (api, service, context);
		// load space model
		ApiResource resource;
		try {
			resource = api.getResourcesManager ().get (SpaceModel);
		} catch (ApiResourcesManagerException ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
		if (resource == null) {
			return;
		}
		InputStream stream = null;
		try {
			stream = resource.toInput ();
			spaceModel = Json.load (stream);
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (stream);
		}
	}

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {

		String 	namespace 	= (String)request.get (Spec.Space);
		
		JsonObject oSpace = (JsonObject)spaceModel.duplicate ().set (ApiSpace.Spec.Namespace, namespace);
		
		// set default secrets
		JsonObject defaultSecrets = Json.getObject (Json.getObject (oSpace, ApiSpace.Spec.secrets.class.getSimpleName ()), ApiSpace.Secrets.Default);
		if (defaultSecrets != null) {
			defaultSecrets.set (ApiSpace.Spec.secrets.Key, Lang.UUID (16));
		}
		
		// create space
		ApiSpace newSpace = null;
		try {
			newSpace = api.space ().create (oSpace);
		} catch (ApiManagementException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		// create root keys
		Map<String, Object> properties = new HashMap<String, Object> ();
		properties.put (CommonSpec.Role, Role.ADMIN.name ());
		
		List<KeyPair> keys = null;
		
		try {
			keys = newSpace.keystore ().create (1, null, properties);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		JsonObject result = newSpace.describe (DescribeOption.Info);
		if (keys != null) {
			result.set (CommonOutput.Keys, keys.get (0).toJson ());
		}
		
		return new JsonApiOutput (result);
	}

}

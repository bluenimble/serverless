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
package com.bluenimble.platform.apis.mgm.spis.storage;

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
import com.bluenimble.platform.apis.mgm.CommonOutput;
import com.bluenimble.platform.apis.mgm.CommonSpec;
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.storage.Storage;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.storage.StorageObject;

public class DeleteObjectSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	interface Spec {
		String Object 	= "object";
		String Force	= "force";
	}

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		ApiSpace space;
		try {
			space = MgmUtils.space (consumer, api);
		} catch (ApiAccessDeniedException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.FORBIDDEN);
		}
		
		String 	provider 	= (String)request.get (CommonSpec.Provider);
		
		Storage storage = space.feature (Storage.class, provider, request);
		
		String path = (String)request.get (Spec.Object);
		
		Boolean force = (Boolean)request.get (Spec.Force);
		if (force == null) {
			force = false;
		}
		
		StorageObject so = null;
		try {
			so = storage.root ().get (path);
		} catch (StorageException e) {
			throw new ApiServiceExecutionException ("storage object '" + path + "' not found", e).status (ApiResponse.NOT_FOUND);
		}

		long count = so.count ();
		
		if (so.isFolder () && count > 0 && !force) {
			throw new ApiServiceExecutionException ("folder " + path + " isn't empty! It can't be deleted").status (ApiResponse.BAD_REQUEST);
		}
		
		boolean deleted = false;
		try {
			deleted = so.delete ();
		} catch (StorageException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.NOT_FOUND);
		}

		return new JsonApiOutput ((JsonObject)new JsonObject ().set (CommonOutput.Deleted, deleted));
	}

}

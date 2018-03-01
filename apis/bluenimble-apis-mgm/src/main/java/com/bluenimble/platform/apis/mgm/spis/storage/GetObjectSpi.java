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

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiAccessDeniedException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.apis.mgm.CommonSpec;
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.apis.mgm.utils.StorageUtils;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.Storage;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.storage.StorageObject;

public class GetObjectSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;
	
	interface Spec {
		String Object = "object";
		String Filter = "filter";
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
		
		String path 	= (String)request.get (Spec.Object);
		
		String filter 	= (String)request.get (Spec.Filter);

		Folder root = null;
				
		StorageObject so = null;
		try {
			root = storage.root ();
			so = root;
		} catch (StorageException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		try {
			if (!Lang.isNullOrEmpty (path) && !path.trim ().equals (Lang.DOT)) {
				so = root.get (path);
			}
		} catch (StorageException e) {
			throw new ApiServiceExecutionException ("storage object '" + path + "' not found", e).status (ApiResponse.NOT_FOUND);
		}
		
		try {
			return so.toOutput (StorageUtils.guessFilter (filter), null, null);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
	}

}

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
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.impls.ResourceApiOutput;
import com.bluenimble.platform.api.security.ApiConsumer;

public class GetResourceApiServiceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = 1283296736684087088L;
	
	interface Spec {
		String Path = "path";
	}

	interface Custom {
		String Folder 		= "folder";
	}

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException {
		
		String path = (String)request.get (Spec.Path);
		
		if (Lang.isNullOrEmpty (path)) {
			throw new ApiServiceExecutionException ("Resource / not found").status (ApiResponse.BAD_REQUEST);
		}
		
		String location = Json.getString (request.getService ().getSpiDef (), Custom.Folder);
		if (!Lang.isNullOrEmpty (location)) {
			path = location + Lang.SLASH + path;
		}
		
		ApiResource r;
		try {
			r = api.getResourcesManager ().get (Lang.split (path, Lang.SLASH));
		} catch (ApiResourcesManagerException e) {
			throw new ApiServiceExecutionException (e.getMessage ()).status (ApiResponse.BAD_REQUEST);
		}
		if (r == null) {
			throw new ApiServiceExecutionException ("Resource " + path + " not found").status (ApiResponse.NOT_FOUND);
		}
		return new ResourceApiOutput (r);
	}
	
}
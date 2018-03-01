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

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.storage.Storage;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.storage.StorageObject;

public class PutStorageObjectApiServiceSpi extends AbstractStorageApiServiceSpi {

	private static final long serialVersionUID = 1283296736684087088L;
	
	private String streamParameter = ApiRequest.Payload;
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException {
		
		Storage storage = api.space ().feature (Storage.class, provider, request);
		
		String path = (String)request.get (objectParameter);
		if (Lang.isNullOrEmpty (path)) {
			throw new ApiServiceExecutionException ("missing object path parameter '" + objectParameter + "'").status (ApiResponse.BAD_REQUEST);
		}
		
		String objectName = null;
		
		StorageObject so = null;
		long length = -1;
		
		ApiStreamSource 	stream 	= null;
		try {
			stream 	= (ApiStreamSource)request.get (streamParameter, Scope.Stream);
			
			if (Lang.isNullOrEmpty (stream.name ())) {
				objectName = Lang.UUID (20);
			}

			so = findFolder (storage.root (), this.folder).add (stream, objectName, true);
			
			length = so.length ();
		} catch (StorageException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		return new JsonApiOutput (
			(JsonObject)new JsonObject ()
				.set (StorageObject.Fields.Name, so.name ())
				.set (StorageObject.Fields.Timestamp, Lang.toUTC (so.timestamp ()))
				.set (StorageObject.Fields.Length, length)
		);

	}
	
	public String getStreamParameter () {
		return streamParameter;
	}
	public void setStreamParameter (String streamParameter) {
		this.streamParameter = streamParameter;
	}

}
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
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException {
		Storage storage = feature (
			api,
			Storage.class, 
			Json.getString (request.getService ().getSpiDef (), Spec.Feature), 
			request
		);
		
		String folder = Json.getString (request.getService ().getSpiDef (), Spec.Folder);
		if (Lang.isNullOrEmpty (folder)) {
			throw new ApiServiceExecutionException ("missing folder path in spi").status (ApiResponse.BAD_REQUEST);
		}
		
		String 	objectName = (String)request.get (Spec.ObjectName);
		Boolean overwrite = (Boolean)request.get (Spec.Overwrite);
		
		if (overwrite == null) {
			overwrite = Json.getBoolean (request.getService ().getSpiDef (), Spec.Overwrite, true);
		}
		
		StorageObject so = null;
		long length = -1;
		
		ApiStreamSource 	stream 	= null;
		try {
			stream 	= (ApiStreamSource)request.get (Spec.StreamId, Scope.Stream);
			
			so = findFolder (storage.root (), folder).add (stream, objectName, overwrite);
			
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

}
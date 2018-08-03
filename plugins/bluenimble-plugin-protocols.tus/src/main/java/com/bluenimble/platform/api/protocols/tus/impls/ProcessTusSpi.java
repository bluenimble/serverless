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
package com.bluenimble.platform.api.protocols.tus.impls;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.protocols.tus.RecyclableTusService;
import com.bluenimble.platform.plugins.protocols.tus.TusProtocolPlugin;
import com.bluenimble.platform.plugins.protocols.tus.impl.TusFileUploadService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.utils.TusUtils;
import com.bluenimble.platform.storage.StorageObject;

public class ProcessTusSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = 2532538390276936715L;
	
	interface Spec {
		String Id 				= "id";
	}
	
	private TusProtocolPlugin plugin;
	
	public ProcessTusSpi (TusProtocolPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject spi = request.getService ().getSpiDef ();
		
		String tusKey = Json.getString (spi, TusProtocolPlugin.Spec.Tus);
		if (Lang.isNullOrEmpty (tusKey)) {
			throw new ApiServiceExecutionException (
				TusProtocolPlugin.Spec.Tus + " not found in service " + ApiService.Spec.Spi.class.getSimpleName ().toLowerCase ()
			).status (ApiResponse.NOT_IMPLEMENTED);
		}
		
		RecyclableTusService tsr = (RecyclableTusService)api.space ().getRecyclable (plugin.createKey (tusKey));
		if (tsr == null) {
			throw new ApiServiceExecutionException (
				TusProtocolPlugin.Spec.Tus + " service " + tusKey + " not found in space"
			).status (ApiResponse.NOT_IMPLEMENTED);
		}
		
		String tenant = null;
		
		switch (tsr.tenantPlaceholder ()) {
			case consumer:
				tenant = (String)consumer.get (tsr.tenantKey ());
				break;
			case request:
				tenant = (String)request.get (tsr.tenantKey ());
				break;
			case header:
				tenant = (String)request.get (tsr.tenantKey (), Scope.Header);
				break;
			default:
				break;
		}
		
		TusFileUploadService service = tsr.service ();
		
		if (service.getStorageService ().isMultiTenant () && Lang.isNullOrEmpty (tenant)) {
			throw new ApiServiceExecutionException (
				tsr.tenantKey () + " not found in consumer" 
			).status (ApiResponse.NOT_FOUND);
		}
		
		// if it's download
		if (ApiVerb.GET.equals (request.getVerb ())) {
			UploadInfo info = null;
			ApiOutput output = null;
			try {
				info = service.getStorageService ().getUploadInfo (request.getPath (), tenant);
				StorageObject object = service.getStorageService ().getData (info, tenant);
				output = object.toOutput (null, info.getFileName (), info.getFileMimeType ());
			} catch (Exception e) {
				throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.NOT_FOUND);
			}
			
			if (output == null) {
				throw new ApiServiceExecutionException ("object " + (String)request.get (Spec.Id) + " not found").status (ApiResponse.NOT_FOUND);
			}
			
			// set metadata header
			if (info.hasMetadata ()) {
                response.set (ApiHeaders.Tus.UploadMetadata, info.getEncodedMetadata ());
            }
			
			return output.set (ApiOutput.Defaults.Disposition, ApiOutput.Disposition.Attachment);
		}
		
		try {
			service.process (
				TusUtils.toHttpRequest (request), TusUtils.toHttpResponse (response), tenant
			);
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex)
				.status (request.getService ().getVerb ().equals (ApiVerb.HEAD) ? ApiResponse.NOT_FOUND : ApiResponse.INTERNAL_SERVER_ERROR);
		} finally {
			response.commit ();
		}
	
		return null;
	}

}

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
package com.bluenimble.platform.apis.mgm.spis.instance;

import java.io.ByteArrayOutputStream;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.impls.ApiByteArrayOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;

public class DownloadRootKeysSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	interface Output {
		String Name 	= "name";
		String Endpoint = "endpoint";
		String KeysName = "root";
		String KeysExt 	= "keys";
	}
	
	interface Spec {
		String Paraphrase = "paraphrase";
	}
	
	@Override
	public ApiOutput execute (Api api, final ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		String paraphrase = (String)request.get (Spec.Paraphrase);
		
		try {
			KeyPair kp = api.space ().getRootKeys ();
			
			JsonObject oKeys = new JsonObject ();
			oKeys.set (Output.Name, kp.accessKey ());
			oKeys.set (Output.Endpoint, request.getScheme () + "://" + request.getEndpoint () + Lang.SLASH + 
										api.space ().getNamespace () + Lang.SLASH + api.getNamespace ());
			oKeys.set (KeyPair.Fields.AccessKey, kp.accessKey ());
			oKeys.set (KeyPair.Fields.SecretKey, kp.secretKey ());
			
			ByteArrayOutputStream out = new ByteArrayOutputStream ();
			
			Json.encrypt (oKeys, paraphrase, out);
			
			return new ApiByteArrayOutput (Output.KeysName + Lang.DOT + Output.KeysExt, Base64.encodeBase64 (out.toByteArray ()), ApiContentTypes.Stream, Output.KeysExt)
					.set (ApiOutput.Defaults.Disposition, "attachment");
			
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage ()).status (ApiResponse.FORBIDDEN);
		}
		
	}

}

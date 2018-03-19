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

import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiAccessDeniedException;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.ApiByteArrayOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.apis.mgm.CommonSpec;
import com.bluenimble.platform.apis.mgm.Role;
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.EncryptionProviderException;
import com.bluenimble.platform.security.KeyPair;

public class GetKeysSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	interface Output {
		String Name 	= "name";
		String Endpoint = "endpoint";
		String Space 	= "space";
		String Domain 	= "domain";
		String KeysExt 	= "keys";
	}
	
	interface Spec {
		String Paraphrase 	= "paraphrase";
	}
	
	@Override
	public ApiOutput execute (Api api, final ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		String accessKey 	= (String)request.get (ApiConsumer.Fields.AccessKey);
		String paraphrase 	= (String)request.get (Spec.Paraphrase);
		
        if (!MgmUtils.isSecure (request.getService ()) ) {
			return getNotSecure (api, request, accessKey, paraphrase);
		}
		
		String role 		= (String)consumer.get (CommonSpec.Role);
		
		String cAccessKey 	= (String)consumer.get (ApiConsumer.Fields.AccessKey);
		
		ApiSpace keysSpace = null;
		KeyPair kp;
		
		// if consumer is super
		try {
			if (Role.SUPER.name ().equals (role.toUpperCase ())) {
				// If super is calling this service, accessKey should be prefixed by space namespace
				int indexOfDot = accessKey.indexOf (Lang.DOT);
				if (indexOfDot <= 0) {
					throw new ApiServiceExecutionException ("invalid accessKey. Using super privileges, you should prefix the accessKey by the space.").status (ApiResponse.BAD_REQUEST);
				}
				String space 	= accessKey.substring (0, indexOfDot);
				accessKey 		= accessKey.substring (indexOfDot + 1);
				keysSpace 		= api.space ().space (space);
			} else {
				keysSpace 		= MgmUtils.space (consumer, api);
			}
		} catch (Exception e) {
			throw new ApiServiceExecutionException ("access denied. " + e.getMessage (), e).status (ApiResponse.FORBIDDEN);
		}
		
		try {
			kp = keysSpace.keystore ().get (accessKey, true);
		} catch (Exception e) {
			throw new ApiServiceExecutionException ("can't access space keystore").status (ApiResponse.FORBIDDEN);
		}
		if (kp == null) {
			throw new ApiServiceExecutionException ("accessKey " + accessKey + " not found").status (ApiResponse.NOT_FOUND);
		}
		
		if (cAccessKey.equals (keysSpace.getNamespace () + Lang.DOT + accessKey)) {
			try {
				return toOutput (kp, paraphrase, keysSpace, api, request);
			} catch (Exception e) {
				throw new ApiServiceExecutionException (e.getMessage (), e);
			}
		}
		
		String keysRole = (String)kp.property (CommonSpec.Role);
		
		if (Role.DEVELOPER.name ().equals (role.toUpperCase ())) {
			throw new ApiServiceExecutionException ("access denied").status (ApiResponse.FORBIDDEN);
		}
		
		if (Role.ADMIN.name ().equals (role.toUpperCase ()) && Role.ADMIN.name ().equals (keysRole.toUpperCase ())) {
			throw new ApiServiceExecutionException ("access denied. only super keys can read ADMIN keys").status (ApiResponse.FORBIDDEN);
		}
		
		try {
			return toOutput (kp, paraphrase, keysSpace, api, request);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
	}
	
	private ApiOutput toOutput (KeyPair kp, String paraphrase, ApiSpace keysSpace, Api api, ApiRequest request) throws EncryptionProviderException {
		JsonObject oKeys = new JsonObject ();
		oKeys.set (Output.Name, keysSpace.getName ());
		oKeys.set (Output.Space, keysSpace.getNamespace ());
		oKeys.set (Output.Endpoint, request.getScheme () + "://" + request.getEndpoint () + Lang.SLASH + 
									api.space ().getNamespace () + Lang.SLASH + api.getNamespace ());
		oKeys.set (Output.Domain, request.getScheme () + "://" + request.getEndpoint () + Lang.SLASH + 
				keysSpace.getNamespace ());
		if (kp.expiryDate () != null) {
			oKeys.set (KeyPair.Fields.ExpiryDate, Lang.toUTC (kp.expiryDate ()));
		}
		oKeys.set (KeyPair.Fields.AccessKey, kp.accessKey ());
		oKeys.set (KeyPair.Fields.SecretKey, kp.secretKey ());
		
		Iterator<String> props = kp.properties ();
		if (props != null) {
			JsonObject oProps = new JsonObject ();
			oKeys.set (KeyPair.Fields.Properties, oProps);
			while (props.hasNext ()) {
				String p = props.next ();
				oProps.set (p, kp.property (p));
			}
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		
		Json.encrypt (oKeys, paraphrase, out);
		
		return new ApiByteArrayOutput (
			keysSpace.getNamespace () + Lang.DOT + Output.KeysExt, Base64.encodeBase64 (out.toByteArray ()), 
			ApiContentTypes.Stream, Output.KeysExt
		)
			.set (ApiOutput.Defaults.Disposition, "attachment");
	}
	
	private ApiOutput getNotSecure (Api api, ApiRequest request, String accessKey, String paraphrase) throws ApiServiceExecutionException {
		ApiSpace keysSpace = null;
		
		int indexOfDot = accessKey.indexOf (Lang.DOT);
		if (indexOfDot <= 0) {
			throw new ApiServiceExecutionException ("invalid accessKey. Using super privileges, you should prefix the accessKey by the space.").status (ApiResponse.BAD_REQUEST);
		}
		String space 	= accessKey.substring (0, indexOfDot);
		accessKey 		= accessKey.substring (indexOfDot + 1);
		try {
			keysSpace 		= api.space ().space (space);
		} catch (ApiAccessDeniedException e) {
			throw new ApiServiceExecutionException ("access denied").status (ApiResponse.FORBIDDEN);
		}
		
		try {
			return toOutput (keysSpace.keystore ().get (accessKey, true), paraphrase, keysSpace, api, request);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
	}

}

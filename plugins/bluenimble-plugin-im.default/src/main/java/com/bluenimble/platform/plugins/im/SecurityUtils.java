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
package com.bluenimble.platform.plugins.im;

import java.util.Date;

import com.bluenimble.platform.Crypto;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiSpace.Endpoint;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class SecurityUtils {
	
	interface Schemes {
		String Token 	= "token";
		String Cookie 	= "cookie";
	}

	public static String [] tokenAndExpiration (Api api, JsonObject entity, Date now, long age) throws ApiServiceExecutionException {
		
		String thing = salt (api, entity);
		
		JsonObject auth = (JsonObject)Json.find (api.getSecurity (), Api.Spec.Security.Schemes, Schemes.Token, Api.Spec.Security.Auth);
		if (auth == null) {
			auth = (JsonObject)Json.find (api.getSecurity (), Api.Spec.Security.Schemes, Schemes.Cookie, Api.Spec.Security.Auth);
		}
		
		String secretsName = Json.getString (auth, ApiSpace.Spec.secrets.class.getSimpleName (), ApiSpace.Secrets.Default);
		
		JsonObject secrets;
		try {
			secrets = api.space ().getSecrets (secretsName);
		} catch (ApiManagementException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		if (secrets == null || !secrets.containsKey (ApiSpace.Spec.secrets.Key)) {
			throw new ApiServiceExecutionException ("space secrets '" + secretsName + "' not found").status (ApiResponse.SERVICE_UNAVAILABLE);
		}
		
		Crypto.Algorithm alg = null;
		
		try {
			alg = Crypto.Algorithm.valueOf (Json.getString (secrets, ApiSpace.Spec.secrets.Algorithm, Crypto.Algorithm.AES.name ()).toUpperCase ());
		} catch (Exception ex) {
			alg = Crypto.Algorithm.AES;
		}
		
		if (age == 0) {
			age = Json.getLong (auth, ApiSpace.Spec.secrets.Age, Json.getLong (secrets, ApiSpace.Spec.secrets.Age, 60));
		} 
		age = age  * 60 * 1000;
		
		long expiresOn = now.getTime () + age;

		// encrypt
		String toEncrypt = expiresOn + Lang.SPACE + thing;
		try {
			return new String [] {
				new String (Lang.encodeHex (Crypto.encrypt (toEncrypt.getBytes (), Json.getString (secrets, ApiSpace.Spec.secrets.Key), alg))),
				Lang.toUTC (new Date (expiresOn))
			};
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
	}
	
	private static String salt (Api api, JsonObject entity) {
		JsonArray fields = Json.getArray (api.getSecurity (), Api.Spec.Security.Encrypt);
		if (fields == null || fields.isEmpty ()) {
			return String.valueOf (entity.get (ApiConsumer.Fields.Id));
		}
		StringBuilder sb = new StringBuilder ();
		for (int i = 0; i < fields.count (); i++) {
			Object v = Json.find (entity, Lang.split (String.valueOf (fields.get (i)), Lang.DOT));
			if (v != null) {
				sb.append (String.valueOf (v));
			}
			if (i < (fields.count () - 1)) {
				sb.append (Lang.SEMICOLON);
			}
		}
		String salt = sb.toString ();
		sb.setLength (0);
		
		return salt;
	}

	public static ApiOutput onFinish (Api api, ApiConsumer consumer, ApiRequest pRequest, final JsonObject onFinish, JsonObject account) 
			throws ApiServiceExecutionException {
		
		if (onFinish == null || onFinish.isEmpty ()) {
			return null;
		}
		
		ApiRequest request = api.space ().request (pRequest, consumer, new Endpoint () {
			@Override
			public String space () {
				return Json.getString (onFinish, ApiRequest.Fields.Space, api.space ().getNamespace ());
			}
			@Override
			public String api () {
				return Json.getString (onFinish, ApiRequest.Fields.Api, api.getNamespace ());
			}
			@Override
			public String [] resource () {
				String resource = Json.getString (onFinish, ApiRequest.Fields.Resource);
				if (resource.startsWith (Lang.SLASH)) {
					resource = resource.substring (1);
				}
				if (resource.endsWith (Lang.SLASH)) {
					resource = resource.substring (0, resource.length () - 1);
				}
				if (Lang.isNullOrEmpty (resource)) {
					return null;
				}
				return Lang.split (resource, Lang.SLASH);
			}
			@Override
			public ApiVerb verb () {
				try {
					return ApiVerb.valueOf (
						Json.getString (onFinish, ApiRequest.Fields.Verb, ApiVerb.POST.name ()).toUpperCase ()
					);
				} catch (Exception ex) {
					return ApiVerb.POST;
				}
			}
		});
		
		request.set (ApiRequest.Payload, account);
		
		return api.call (request);
		
	}

}

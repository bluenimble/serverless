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
package com.bluenimble.platform.api.security.impls;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Crypto;
import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.security.ApiConsumerResolver;
import com.bluenimble.platform.api.security.ApiConsumerResolverAnnotation;
import com.bluenimble.platform.api.utils.Features;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.impls.JsonQuery;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.security.impls.DefaultApiConsumer;

@ApiConsumerResolverAnnotation (name = BasicConsumerResolver.Scheme)
public class BasicConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;
	
	protected static final String Scheme 		= "basic";
	
	protected static final String BasicAuth 	= "Basic";
	
	interface Defaults {
		String 	LoginField = "email";
	}
	
	interface Spec {
		String Encryption = "encryption";
		interface Auth {
			String Feature 		= "feature";
			String Query 		= "query";
		}
	}
	
	@Override
	public ApiConsumer resolve (Api api, ApiService service, ApiRequest request)
			throws ApiAuthenticationException {
		
		String authHeader 	= (String)request.get (ApiHeaders.Authorization, Scope.Header);
		if (Lang.isNullOrEmpty (authHeader)) {
			return null;
		}
		
		String [] pair = Lang.split (authHeader, Lang.SPACE, true);
		if (pair.length < 2) {
			return null;
		}
		
		String app 			= pair [0];
		if (!app.equals (BasicAuth)) {
			return null;
		}

		String credentials		= new String (Base64.decodeBase64 (pair [1]));
		String [] aCredentials 	= Lang.split (credentials, Lang.COLON, true);
		if (aCredentials == null || aCredentials.length < 2) {
			return null;
		}
		
		ApiConsumer consumer = new DefaultApiConsumer (ApiConsumer.Type.Basic);
		consumer.set (ApiConsumer.Fields.Id, aCredentials [0]);
		consumer.set (ApiConsumer.Fields.Password, aCredentials [1]);
		
		return consumer;
	}

	@Override
	public ApiConsumer authorize (Api api, ApiService service, ApiRequest request, ApiConsumer consumer)
			throws ApiAuthenticationException {
		
		JsonObject scheme = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), Scheme);
		if (Json.isNullOrEmpty (scheme)) {
			return consumer;
		}
		
		JsonObject auth = Json.getObject (scheme, Api.Spec.Security.Auth);
		if (Json.isNullOrEmpty (auth)) {
			return consumer;
		}
		
		String 		feature 	= Json.getString (auth, Spec.Auth.Feature);
		JsonObject 	query 		= Json.getObject (auth, Spec.Auth.Query);
		
		if (query == null || query.isEmpty ()) {
			return consumer;
		}
		
		String password = (String)consumer.get (ApiConsumer.Fields.Password);
		
		String encrytionAlgorithm = Json.getString (scheme, Spec.Encryption);
		if (!Lang.isNullOrEmpty (encrytionAlgorithm)) {
			try {
				password = Crypto.md5 (password, Encodings.UTF8);
			} catch (UnsupportedEncodingException ex) {
				throw new ApiAuthenticationException (ex.getMessage (), ex);
			}
		}
		
		Map<String, Object> bindings = new HashMap<String, Object> ();
		bindings.put (ApiConsumer.Fields.Id, consumer.get (ApiConsumer.Fields.Id));
		bindings.put (ApiConsumer.Fields.Password, password);
		
		JsonQuery q = new JsonQuery (query, bindings);
		
		DatabaseObject odb = null;
		try {
			odb = Features.get (api, Database.class, feature, request).findOne (null, q);
		} catch (Exception ex) {
			throw new ApiAuthenticationException (ex.getMessage (), ex);
		}
		
		boolean isServiceSecure = Json.getBoolean (service.getSecurity (), ApiService.Spec.Security.Enabled, true);
		
		if (odb == null) {
			if (isServiceSecure) {
				throw new ApiAuthenticationException ("invalid user/password");
			} else {
				return consumer;
			}
		}
		
		JsonObject oConsumer = odb.toJson (null);
		
		for (Object k : oConsumer.keySet ()) {
			consumer.set (String.valueOf (k), oConsumer.get (k));
		}
		
		consumer.set (ApiConsumer.Fields.Anonymous, false);

		return consumer;
	}
	
}

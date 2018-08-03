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

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.impls.JsonQuery;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.security.impls.DefaultApiConsumer;

@ApiConsumerResolverAnnotation (name = KeyConsumerResolver.Scheme)
public class KeyConsumerResolver implements ApiConsumerResolver {

	private static final long serialVersionUID = 889277317993642120L;

	protected static final String Scheme = "key";

	interface Defaults {
		String 	Prefix 				= "BNB-API-Key";
		long 	Validity 			= 300;
		String 	TimestampHeader 	= ApiHeaders.Timestamp;
		String	AccessKey			= "uuid";
		String	SecretKey			= "secretKey";
	}
	
	interface Spec {
		String 	Prefix 				= "prefix";
		String 	Validity 			= "validity";
		String 	TimestampHeader 	= "timestampHeader";
		interface Auth {
			String 	Feature 		= "feature";
			String 	Query 			= "query";
			String 	Parameters		= "parameters";
			String 	SecretKeyField	= "secretKeyField";
		}
	}

	@Override
	public ApiConsumer resolve (Api api, ApiService service, ApiRequest request)
			throws ApiAuthenticationException {
		
		JsonObject oResolver = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), Scheme);
		
		String 	scheme 	= Json.getString 	(oResolver, Spec.Prefix, Defaults.Prefix);
		
		String auth = (String)request.get (ApiHeaders.Authorization, Scope.Header);
		
		if (Lang.isNullOrEmpty (auth)) {
			return null;
		}
		
		String [] pair = Lang.split (auth, Lang.SPACE, true);
		
		if (pair.length < 2) {
			return null;
		}
		
		String rScheme = pair [0];

		if (!rScheme.equals (scheme)) {
			return null;
		}
		
		String accessKeyAndSignature = pair [1];
		if (Lang.isNullOrEmpty (accessKeyAndSignature)) {
			return null;
		}
		
		int indexOfColon = accessKeyAndSignature.indexOf (Lang.COLON);
		if (indexOfColon <= 0) {
			return null;
		}

		String accessKey 	= accessKeyAndSignature.substring (0, indexOfColon);
		String signature 	= accessKeyAndSignature.substring (indexOfColon + 1);
		
		ApiConsumer consumer = new DefaultApiConsumer (ApiConsumer.Type.Signature);
		consumer.set (ApiConsumer.Fields.AccessKey, accessKey);
		consumer.set (ApiConsumer.Fields.Signature, signature);
		
		return consumer;

	}
	
	@Override
	public ApiConsumer authorize (Api api, ApiService service, ApiRequest request, ApiConsumer consumer)
			throws ApiAuthenticationException {
		
		JsonObject oResolver = Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), Scheme);
		
		long 	validity 		= Json.getLong 		(oResolver, Spec.Validity, Defaults.Validity) * 1000;
		String 	timestampHeader = Json.getString 	(oResolver, Spec.TimestampHeader, Defaults.TimestampHeader);
		
		String accessKey = (String)consumer.get (ApiConsumer.Fields.AccessKey);
		if (Lang.isNullOrEmpty (accessKey)) {
			throw new ApiAuthenticationException ("Invalid request. Invalid consumer " + accessKey);
		}

		String timestamp = (String)request.get (timestampHeader, Scope.Header);
		if (Lang.isNullOrEmpty (timestamp)) {
			throw new ApiAuthenticationException ("No timestamp specified");
		}
		
		String signature = (String)consumer.get (ApiConsumer.Fields.Signature);
		if (Lang.isNullOrEmpty (signature)) {
			throw new ApiAuthenticationException ("Unsigned request");
		}

		String secretKey = (String)consumer.get (ApiConsumer.Fields.SecretKey);
		if (Lang.isNullOrEmpty (secretKey)) {
			secretKey = getSecretKey (api, request, consumer, accessKey);
		}
		
		if (Lang.isNullOrEmpty (secretKey)) {
			throw new ApiAuthenticationException ("Invalid consumer " + accessKey);
		}
		
		Object oExpiryDate = consumer.get (ApiConsumer.Fields.ExpiryDate);
		if (oExpiryDate != null) {
			Date expiryDate = null;
			if (oExpiryDate instanceof Date) {
				expiryDate = (Date)oExpiryDate;
			} else if (oExpiryDate instanceof String) {
				try {
					expiryDate = Lang.toDate ((String)oExpiryDate, Lang.DEFAULT_DATE_FORMAT);
				} catch (Exception ex) { 
					throw new ApiAuthenticationException (ex.getMessage (), ex);
				}
			} else {
				throw new ApiAuthenticationException ("unsupported expiry date format found on cunsumer " + oExpiryDate.getClass ());
			}
			if (expiryDate.before (new Date ())) {
				throw new ApiAuthenticationException ("No timestamp specified");
			}
		}
		
		Date time;
		try {
			time = Lang.toUTC (timestamp);
		} catch (ParseException e) {
			throw new ApiAuthenticationException ("Bad timestamp format. Use UTC [" + Lang.UTC_DATE_FORMAT + "]");
		}
		
		if (time == null) {
			throw new ApiAuthenticationException ("Bad timestamp format. Use UTC [" + Lang.UTC_DATE_FORMAT + "]");
		}
		
		long elapsed = System.currentTimeMillis () - time.getTime ();
		if (elapsed > validity) {
			throw new ApiAuthenticationException ("Invalid request. Elapsed time must not exceed " + (validity/1000) + " seconds");
		}

		String calculated = null;
		
		try {
			calculated = api.space ().sign (request, timestamp, accessKey, (String)consumer.get (ApiConsumer.Fields.SecretKey), false);
		} catch (Exception ex) {
			throw new ApiAuthenticationException (ex.getMessage (), ex);
		}
		
		api.tracer ().log (Tracer.Level.Info, "{0} -> caldulated signature: {1}", request.getId (), calculated);
		
		if (!signature.equals (calculated)) {
			throw new ApiAuthenticationException ("Invalid signature");
		}
		
		consumer.set (ApiConsumer.Fields.Anonymous, false);
		return consumer;
	}
	
	private String getSecretKey (Api api, ApiRequest request, ApiConsumer consumer, String accessKey) throws ApiAuthenticationException {
		JsonObject auth = Json.getObject (Json.getObject (Json.getObject (api.getSecurity (), Api.Spec.Security.Schemes), Scheme), Api.Spec.Security.Auth);
		if (auth == null || auth.isEmpty ()) {
			return null;
		}
		
		String 		feature 		= Json.getString 	(auth, Spec.Auth.Feature);
		String 		secretKeyField 	= Json.getString 	(auth, Spec.Auth.SecretKeyField, Defaults.SecretKey);
		JsonObject 	query 			= Json.getObject 	(auth, Spec.Auth.Query);
		
		JsonArray 	parameters 		= Json.getArray 	(auth, Spec.Auth.Parameters);
		
		if (query == null || query.isEmpty ()) {
			return null;
		}
		
		Map<String, Object> bindings = new HashMap<String, Object> ();
		bindings.put (ApiConsumer.Fields.AccessKey, accessKey);
		
		// addt params
		if (parameters != null && !parameters.isEmpty ()) {
			for (int i = 0; i < parameters.count (); i++) {
				String key = String.valueOf (parameters.get (i));
				Object o = request.get (key);
				if (o != null) {
					bindings.put (key, o);
				}
			}
		}
		
		JsonQuery q = new JsonQuery (query, bindings);
		
		DatabaseObject odb = null;
		try {
			odb = api.space ().feature (Database.class, feature, request).findOne (null, q);
		} catch (Exception ex) {
			throw new ApiAuthenticationException (ex.getMessage (), ex);
		}
		
		if (odb == null) {
			throw new ApiAuthenticationException ("invalid accessKey " + accessKey);
		}
		
		JsonObject oRecord = odb.toJson (null);
		
		String [] secretKeyProps = Lang.split (secretKeyField, Lang.DOT);
		
		Object oSecretKey = Json.find (oRecord, secretKeyProps);
		
		if (oSecretKey == null) {
			throw new ApiAuthenticationException ("secret key not found for accessKey " + accessKey);
		}

		if (!(oSecretKey instanceof String)) {
			throw new ApiAuthenticationException ("secret key should be a valid String");
		}
		
		consumer.set (ApiConsumer.Fields.AccessKey, accessKey);
		consumer.set (ApiConsumer.Fields.SecretKey, oSecretKey);

		JsonObject oConsumer = oRecord;
		
		for (Object k : oConsumer.keySet ()) {
			consumer.set (String.valueOf (k), oConsumer.get (k));
		}
		
		consumer.set (ApiConsumer.Fields.Anonymous, false);

		return (String)oSecretKey;
	}
	
}

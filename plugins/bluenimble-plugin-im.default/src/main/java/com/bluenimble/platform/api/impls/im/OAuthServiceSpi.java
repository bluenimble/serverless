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
package com.bluenimble.platform.api.impls.im;

import java.io.InputStream;
import java.util.Iterator;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Config;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.http.HttpClientException;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.http.utils.Http;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.im.SecurityUtils;

public class OAuthServiceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -5297356423303847595L;
	
	private static final String Providers = "providers";

	interface OAuth {
		String Keys 		= "keys";
		String Endpoints	= "endpoints";
		
		String Code 		= "code";
		String ClientId 	= "client_id";
		String ClientSecret = "client_secret";
		String RedirectUri	= "redirect_uri";
		String AccessToken 	= "access_token";
		
		String Redirect		= "redirect";
		
		interface Urls {
			String Authorize 	= "authorize";
			String Profile 		= "profile";
			String Email 		= "email";
		}
		
		interface Endpoint {
			String Parameters	= "params";
			String Headers		= "headers";
			String Data			= "data";
			String Url			= "url";
		}
		
	}
	
	interface Spec {
		String Provider = "provider";
		String AuthCode = "authcode";
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject config 		= request.getService ().getSpiDef ();

		JsonObject providers 	= Json.getObject (config, Providers);
		
		JsonObject provider = Json.getObject (providers, (String)request.get (Spec.Provider));
		if (provider == null || provider.isEmpty ()) {
			throw new ApiServiceExecutionException ("provider " + request.get (Spec.Provider) + " not supported").status (ApiResponse.NOT_ACCEPTABLE);
		}
		
		JsonObject oAuthKeys = Json.getObject (provider, OAuth.Keys);
		if (oAuthKeys == null || oAuthKeys.isEmpty ()) {
			throw new ApiServiceExecutionException ("provider " + request.get (Spec.Provider) + ". client_id and client_secret not found").status (ApiResponse.NOT_ACCEPTABLE);
		}
		
		JsonObject oAuthEndpoints = Json.getObject (provider, OAuth.Endpoints);
		if (oAuthEndpoints == null || oAuthEndpoints.isEmpty ()) {
			throw new ApiServiceExecutionException ("provider " + request.get (Spec.Provider) + ". oAuth endpoints authorize and profile not configured").status (ApiResponse.NOT_ACCEPTABLE);
		}
		
		JsonObject endpoint = Json.getObject (oAuthEndpoints, OAuth.Urls.Authorize);
		if (endpoint == null || endpoint.isEmpty ()) {
			throw new ApiServiceExecutionException ("provider " + request.get (Spec.Provider) + ". oAuth authorize endpoint not configured").status (ApiResponse.NOT_ACCEPTABLE);
		}
		
		JsonObject data = (JsonObject)new JsonObject ()
			.set (OAuth.Code, 			request.get (Spec.AuthCode))	
			.set (OAuth.ClientId, 		Json.getString (oAuthKeys, OAuth.ClientId))	
			.set (OAuth.ClientSecret, 	Json.getString (oAuthKeys, OAuth.ClientSecret));
		
		if (provider.containsKey (OAuth.Redirect)) {
			data.set (OAuth.RedirectUri, Json.getString (provider, OAuth.Redirect));
		}
		
		JsonObject params = Json.getObject (endpoint, OAuth.Endpoint.Parameters);
		if (params != null && !params.isEmpty ()) {
			Iterator<String> keys = params.keys ();
			while (keys.hasNext ()) {
				String p = keys.next ();
				data.set (p, params.get (p));
			}
		}
		
		JsonObject hRequest = (JsonObject)new JsonObject ()
				.set (OAuth.Endpoint.Url, Json.getString (endpoint, OAuth.Endpoint.Url))
				.set (OAuth.Endpoint.Headers, new JsonObject ().set (HttpHeaders.ACCEPT, ApiContentTypes.Json))
				.set (OAuth.Endpoint.Data, data);
		
		HttpResponse hResponse = null;
		try {
			hResponse = Http.post (hRequest, null);
		} catch (HttpClientException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		if (hResponse.getStatus () != 200) {
			throw new ApiServiceExecutionException ("invalid authorization code");
		}
		
		InputStream out = hResponse.getBody ().get (0).toInputStream ();
		
		JsonObject oAuthResult = null;
		try {
			oAuthResult = new JsonObject (out);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		} finally {
			IOUtils.closeQuietly (out);
		}
		
		// get profile
		endpoint = Json.getObject (oAuthEndpoints, OAuth.Urls.Profile);
		if (endpoint == null || endpoint.isEmpty ()) {
			return new JsonApiOutput (oAuthResult);
		}
		
		String accessToken = Json.getString (oAuthResult, OAuth.AccessToken);
		
		data.clear ();
		data.set (OAuth.AccessToken, accessToken);
		
		hRequest = (JsonObject)new JsonObject ()
				.set (OAuth.Endpoint.Url, Json.getString (endpoint, OAuth.Endpoint.Url))
				.set (OAuth.Endpoint.Headers, new JsonObject ().set (HttpHeaders.ACCEPT, ApiContentTypes.Json))
				.set (OAuth.Endpoint.Data, data);
		
		try {
			hResponse = Http.post (hRequest, null);
		} catch (HttpClientException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		if (hResponse.getStatus () != 200) {
			throw new ApiServiceExecutionException ("invalid access token");
		}
		
		out = hResponse.getBody ().get (0).toInputStream ();
		
		try {
			oAuthResult = new JsonObject (out);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		} finally {
			IOUtils.closeQuietly (out);
		}
		
		// email endpoint
		endpoint = Json.getObject (oAuthEndpoints, OAuth.Urls.Email);
		if (endpoint == null || endpoint.isEmpty ()) {
			return new JsonApiOutput (oAuthResult);
		}
		
		hRequest = (JsonObject)new JsonObject ()
				.set (OAuth.Endpoint.Url, Json.getString (endpoint, OAuth.Endpoint.Url))
				.set (OAuth.Endpoint.Headers, new JsonObject ().set (HttpHeaders.ACCEPT, ApiContentTypes.Json))
				.set (OAuth.Endpoint.Data, data);
		
		try {
			hResponse = Http.post (hRequest, null);
		} catch (HttpClientException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		if (hResponse.getStatus () != 200) {
			throw new ApiServiceExecutionException ("invalid access token");
		}
		
		out = hResponse.getBody ().get (0).toInputStream ();
		
		JsonObject oEmail = null;
		try {
			oEmail = new JsonObject (out);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		} finally {
			IOUtils.closeQuietly (out);
		}
		
		Iterator<String> keys = oEmail.keys ();
		while (keys.hasNext ()) {
			String k = keys.next ();
			oAuthResult.set (k, oEmail.get (k));
		}

		// call onFinish if any
		return SecurityUtils.onFinish (
			api, consumer, request, 
			Json.getObject (config, Config.onFinish.class.getSimpleName ()), 
			oAuthResult
		);
		
	}

}
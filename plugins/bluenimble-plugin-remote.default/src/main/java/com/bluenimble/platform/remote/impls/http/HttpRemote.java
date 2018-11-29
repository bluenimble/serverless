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
package com.bluenimble.platform.remote.impls.http;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.utils.ContentTypes;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.Serializer;
import com.bluenimble.platform.remote.impls.BaseRemote;
import com.bluenimble.platform.remote.impls.http.bnb.AccessSecretKeysBasedHttpRequestSigner;
import com.bluenimble.platform.remote.impls.http.oauth.OkHttpOAuthConsumer;
import com.bluenimble.platform.templating.SimpleVariableResolver;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRemote extends BaseRemote {
	
	private static final long serialVersionUID = -4470909404236824873L;

	private static final String DefaultUserAgent 	= "BlueNimble Http Client";
	
	public interface Pool {
		String MaxIdleConnections = "maxIdleConnections";
		String KeepAliveDuration = "keepAliveDuration";
	}
	
	protected static final Map<String, MediaType> MediaTypes 	= new HashMap<String, MediaType> ();
	static {
		// media types
		MediaTypes.put (ContentTypes.Json, MediaType.parse (ContentTypes.Json));
		MediaTypes.put (Serializer.Name.json.name (), MediaTypes.get (ContentTypes.Json));
		MediaTypes.put (ContentTypes.FormUrlEncoded, MediaType.parse (ContentTypes.FormUrlEncoded));
		MediaTypes.put (ContentTypes.Multipart, MediaType.parse (ContentTypes.Multipart));
		MediaTypes.put (ContentTypes.Text, MediaType.parse (ContentTypes.Text));
	}
	
	private static final AccessSecretKeysBasedHttpRequestSigner BnBSigner = new AccessSecretKeysBasedHttpRequestSigner ();
	
	private OkHttpClient http;
	
	private JsonObject 	featureSpec;
	
	public HttpRemote (ApiSpace space, String feature, JsonObject featureSpec, OkHttpClient http) {
		this.featureSpec 	= featureSpec;
		this.http 			= http;
	}
 
	@Override
	public void request (ApiVerb verb, JsonObject spec, Callback callback, ApiStreamSource... attachments) {
	
		if (http == null) {
			try {
				callback.onError (Error.Other, "http client not initialized");
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
		}
		
		JsonObject rdata = Json.getObject (spec, Spec.Data);
		
		if (!Json.isNullOrEmpty (featureSpec)) {
			JsonObject master = featureSpec.duplicate ();
			
			Json.resolve (master, ECompiler, new SimpleVariableResolver () {
				private static final long serialVersionUID = 1L;
				@Override
				public Object resolve (String namespace, String... property) {
					Object v = Json.find (rdata, property);
					Json.remove (rdata, property);
					return v;
				}
			});
			
			spec = master.merge (spec);
		}
		
		String endpoint = Json.getString (spec, Spec.Endpoint);
		
		String path = Json.getString (spec, Spec.Path);
		if (!Lang.isNullOrEmpty (path)) {
			if (!path.startsWith (Lang.SLASH) && !endpoint.endsWith (Lang.SLASH)) {
				path = Lang.SLASH + path;
			}
			endpoint += path;
		}
		
		Serializer.Name serName = null;
		
		try {
			serName = Serializer.Name.valueOf (Json.getString (spec, Spec.Serializer, Serializer.Name.text.name ()).toLowerCase ());
		} catch (Exception ex) {
			// ignore
			serName = Serializer.Name.text;
		}
		
		Serializer serializer = Serializers.get (serName);
		
		Request request = null;
		
		Response response = null;
		
		try {
			
			// contentType
			String contentType = null;

			// resole and add headers
			JsonObject headers = Json.getObject (spec, Spec.Headers);
			if (!Json.isNullOrEmpty (headers)) {
				Json.resolve (headers, ECompiler, new SimpleVariableResolver () {
					private static final long serialVersionUID = 1L;
					@Override
					public Object resolve (String namespace, String... property) {
						return Json.find (rdata, property);
					}
				});
				Iterator<String> hnames = headers.keys ();
				while (hnames.hasNext ()) {
					String hn = hnames.next ();
					String hv = Json.getString (headers, hn);
					if (HttpHeaders.CONTENT_TYPE.toUpperCase ().equals (hn.toUpperCase ())) {
						contentType = hv;
						break;
					}
				}
			}
			
			if (Lang.isNullOrEmpty (contentType)) {
				contentType = ContentTypes.FormUrlEncoded;
			}

			contentType = contentType.trim ();

			MediaType mediaType = MediaTypes.get (contentType);

			RequestBody body = null;
			
			List<RequestParameter> parameters = null;

			if (attachments != null && attachments.length > 0 && !Json.isNullOrEmpty (rdata)) {
				// multipart body
				MultipartBody.Builder builder = new MultipartBody.Builder ();
				Iterator<String> pnames = rdata.keys ();
				while (pnames.hasNext ()) {
					String pn = pnames.next ();
					builder.addFormDataPart (pn, String.valueOf (rdata.get (pn)));
				}
				
				for (ApiStreamSource ss : attachments) {
					try {
						builder.addFormDataPart (ss.name (), ss.name (), RequestBody.create (MediaType.parse (contentType), IOUtils.toByteArray (ss.stream ())));
					} catch (Exception ex) {
						callback.onError (Error.Other, ex.getMessage ());
						return;
					}
				}
			} else if (contentType.startsWith (ContentTypes.Json)) {
				body = RequestBody.create (
					mediaType, rdata == null ? Lang.EMTPY_OBJECT : rdata.toString (Json.getBoolean (spec, Spec.Cast, false))
				);
			} else {
				if (!Json.isNullOrEmpty (rdata)) {
					
					// for bnb signature only
					if (Signers.Bnb.equals (Json.find (spec, Spec.Sign, Spec.SignProtocol))) {
						parameters = new ArrayList<RequestParameter> ();
					}
					
					if (verb.equals (ApiVerb.POST) || verb.equals (ApiVerb.PUT) || verb.equals (ApiVerb.PATCH)) {
						FormBody.Builder fb = new FormBody.Builder ();

						Iterator<String> pnames = rdata.keys ();
						while (pnames.hasNext ()) {
							String pn = pnames.next ();
							fb.add (pn, String.valueOf (rdata.get (pn)));
							if (parameters != null) {
								parameters.add (new RequestParameter (pn, rdata.get (pn)));
							}
						}

						body = fb.build ();
					} else if (verb.equals (ApiVerb.GET)) {
						HttpUrl.Builder urlBuilder = HttpUrl.parse (endpoint).newBuilder ();
						Iterator<String> pnames = rdata.keys ();
						while (pnames.hasNext ()) {
							String pn = pnames.next ();
							urlBuilder.addQueryParameter (pn, String.valueOf (rdata.get (pn)));
							if (parameters != null) {
								parameters.add (new RequestParameter (pn, rdata.get (pn)));
							}
						}
						endpoint = urlBuilder.build ().toString ();
					}
				}
			}

			// create the request builder
			Request.Builder rBuilder = new Request.Builder ().url (endpoint);
			rBuilder.header (HttpHeaders.USER_AGENT, DefaultUserAgent);
			
			// add headers
			if (!Json.isNullOrEmpty (headers)) {
				Iterator<String> hnames = headers.keys ();
				while (hnames.hasNext ()) {
					String hn = hnames.next ();
					String hv = Json.getString (headers, hn);
					rBuilder.header (hn, hv);
				}
			}
			
			// create request
			switch (verb) {
				case GET:
					rBuilder.get ();
					break;
				case POST:
					rBuilder.post (body);
					break;
				case DELETE:
					rBuilder.delete ();
					break;	
				case PUT:
					rBuilder.put (body);
					break;
				case PATCH:
					rBuilder.patch (body);
					break;	
				case HEAD:
					rBuilder.head ();
					break;	
				default:
					break;
			}

			// build then sign
			request = sign (rBuilder.build (), spec, parameters);
			
			response = http.newCall (request).execute ();
			
			Headers rHttpHeaders = response.headers ();
			Set<String> hNames = rHttpHeaders.names ();
			if (hNames != null && !hNames.isEmpty ()) {
				Map<String, Object> cHeaders = new HashMap<String, Object> ();
				for (String hn : hNames) {
					cHeaders.put (hn, rHttpHeaders.get (hn));
				}
				callback.onStatus (response.code (), false, cHeaders);
			}
						
			if (response.code () > Json.getInteger (spec, Spec.SuccessCode, 399)) {
				callback.onError (
					response.code (), 
					response.body ().string ()
				);
				return;
			} else {
				callback.onDone (
					response.code (), 
					serializer.serialize (response.body ().byteStream ())
				);
				return;
			}
			
		} catch (UnknownHostException uhex) {
			try {
				callback.onError (Error.UnknownHost, "Endpoint " + endpoint + " can't be resolved. Check your internet connection and make sure the endpoint is correct");
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
			return;
		} catch (SocketTimeoutException stoex) {
			try {
				callback.onError (Error.Timeout, "Endpoint " + endpoint + " was found but " + stoex.getMessage ());
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
			return;
		} catch (Exception ex) {
			try {
				callback.onError (Error.Other, Lang.toError (ex));
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
			return;
		} finally {
			if (response != null) {
				response.close ();
			}
		}
	}
	
	private Request sign (Request request, JsonObject spec, List<RequestParameter> parameters) throws Exception {
		if (!spec.containsKey (Spec.Sign)) {
			return request;
		}
		
		Object sign = Json.getObject (spec, Spec.Sign);
		if (!(sign instanceof JsonObject)) {
			return request;
		}
		
		JsonObject oSign = (JsonObject)sign;
		
		String signer = Json.getString (oSign, Spec.SignProtocol, Signers.OAuth).toLowerCase ();
		
		if (Signers.OAuth.equals (signer)) {
			String key = Json.getString (oSign, Spec.SignKey);
			if (Lang.isNullOrEmpty (key)) {
				throw new Exception ("oauth consumer key not found in spec");
			}
			String secret = Json.getString (oSign, Spec.SignSecret);
			if (Lang.isNullOrEmpty (secret)) {
				throw new Exception ("oauth consumer secret not found in spec");
			}

			OkHttpOAuthConsumer consumer = 
					new OkHttpOAuthConsumer (key, secret);
			
			String token = Json.getString (oSign, Spec.SignToken);
			if (!Lang.isNullOrEmpty (token)) {
				String tokenSecret = Json.getString (oSign, Spec.SignTokenSecret);
				if (Lang.isNullOrEmpty (tokenSecret)) {
					throw new Exception ("oauth token secret not found in spec");
				}
				consumer.setTokenWithSecret (token, secret);
			}
			return (Request)consumer.sign (request).unwrap ();
		} else if (Signers.Bnb.equals (signer)) {
			// bnb sign
			String key = Json.getString (oSign, Spec.SignKey);
			
			if (Lang.isNullOrEmpty (key)) {
				throw new Exception ("bnb key not found in spec");
			}
			String secret = Json.getString (oSign, Spec.SignSecret);
			if (Lang.isNullOrEmpty (secret)) {
				throw new Exception ("bnb secret not found in spec");
			}
			
			return BnBSigner.sign (request, parameters, oSign);
			
		} else if (Signers.Basic.equals (signer)) {
			// bnb sign
			String user = Json.getString (oSign, Spec.User);
			if (Lang.isNullOrEmpty (user)) {
				throw new Exception ("basic-auth user not found in spec");
			}
			String password = Json.getString (oSign, Spec.Password);
			if (Lang.isNullOrEmpty (password)) {
				password = Lang.BLANK;
			}
			
			return request.newBuilder ().header (
				HttpHeaders.AUTHORIZATION, 
				"Basic " + 
				new String (Base64.encodeBase64String ((user + Lang.COLON + password).getBytes ())).trim ()
	    	).build ();

		} else {
			throw new Exception ("unsupported signature protocol " + signer);
		}
		
	}

	@Override
	public void finish (boolean withError) {
	}

	@Override
	public void recycle () {
	}

}

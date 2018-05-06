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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
import com.bluenimble.platform.remote.impls.AbstractRemote;
import com.bluenimble.platform.remote.impls.http.bnb.AccessSecretKeysBasedHttpRequestSigner;
import com.bluenimble.platform.remote.impls.http.oauth.OkHttpOAuthConsumer;
import com.bluenimble.platform.templating.VariableResolver;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRemote extends AbstractRemote {
	
	private static final long serialVersionUID = -4470909404236824873L;

	private static final String DefaultUserAgent 	= "BlueNimble Http Client";
	
	private HostnameVerifier TrustAllHostVerifier = new HostnameVerifier () {
		@Override
		public boolean verify (String host, SSLSession session) {
			if (!host.equalsIgnoreCase (session.getPeerHost ())) {
                System.out.println ("Warning: URL host '" + host + "' is different than SSLSession host '" + session.getPeerHost () + "'.");
            }
			return true;
		}
	};
	
	private static final X509TrustManager TrustAllManager = new X509TrustManager () {
        public X509Certificate [] getAcceptedIssuers () { return new java.security.cert.X509Certificate[0]; }
        public void checkClientTrusted (X509Certificate[] certs, String authType) { }
        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
    };
	
	private static SSLSocketFactory TrustAllSocketFactory;
	static {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance ("TLS");
			sslContext.init (null, new TrustManager[] { TrustAllManager }, new java.security.SecureRandom ());
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
        TrustAllSocketFactory = sslContext.getSocketFactory ();
	}
	
	private static final OkHttpClient DefaultHttpClient = 
		new OkHttpClient.Builder ()
				.connectTimeout (10, TimeUnit.SECONDS)
				.readTimeout (10, TimeUnit.SECONDS)
				.writeTimeout (10, TimeUnit.SECONDS)
				.build ();
	
	AccessSecretKeysBasedHttpRequestSigner BnBSigner = new AccessSecretKeysBasedHttpRequestSigner ();
	
	private OkHttpClient http = DefaultHttpClient;
	
	private JsonObject 	featureSpec;
	
	public HttpRemote () {
	}

	public HttpRemote (ApiSpace space, String feature, JsonObject featureSpec) {
		this.featureSpec = featureSpec;
		init (space, feature, featureSpec);
	}

	private void init (ApiSpace space, String feature, JsonObject featureSpec) {
		if (Json.isNullOrEmpty (featureSpec)) {
			return;
		}

		OkHttpClient.Builder builder = new OkHttpClient.Builder ();

		// ssl trust all
		if (Json.getBoolean (featureSpec, Spec.TrustAll, false)) {
			builder	.sslSocketFactory (TrustAllSocketFactory, TrustAllManager)
					.hostnameVerifier (TrustAllHostVerifier);
		}
		
		JsonObject oProxy = Json.getObject (featureSpec, Spec.Proxy);
		JsonObject oTimeout = Json.getObject (featureSpec, Spec.Timeout);
		
		int connectTimeout = Json.getInteger (oTimeout, Spec.TimeoutConnect, 0);
		if (connectTimeout > 0) {
			builder.connectTimeout (connectTimeout, TimeUnit.SECONDS);
		}
		
		int writeTimeout = Json.getInteger (oTimeout, Spec.TimeoutWrite, 0);
		if (writeTimeout > 0) {
			builder.connectTimeout (writeTimeout, TimeUnit.SECONDS);
		}
		
		int readTimeout = Json.getInteger (oTimeout, Spec.TimeoutRead, 0);
		if (readTimeout > 0) {
			builder.connectTimeout (readTimeout, TimeUnit.SECONDS);
		}
		
		if (Json.isNullOrEmpty (oProxy)) {
			http = builder.build ();
			return;
		}
		
		// if proxy
		
		Proxy.Type type = null;
		try {
			type = Proxy.Type.valueOf (Json.getString (oProxy, Spec.ProxyType, Proxy.Type.HTTP.name ()).toUpperCase ());
		} catch (Exception ex) {
			type = Proxy.Type.HTTP;
		}
		
		String host = Json.getString 	(oProxy, Spec.ProxyHost);
		int port 	= Json.getInteger 	(oProxy, Spec.ProxyPort, 0);
		if (Lang.isNullOrEmpty (host) || port == 0) {
			http = builder.build ();
			return;
		}
		
		http = builder.proxy (new Proxy (type, new InetSocketAddress (host, port))).build ();
		
		return;
		
	}
 
	@Override
	public boolean get (JsonObject spec, Callback callback) {
		return request (ApiVerb.GET, spec, callback);
	}
	
	@Override
	public boolean post (JsonObject spec, Callback callback, ApiStreamSource... attachments) {
		return request (ApiVerb.POST, spec, callback);
	}

	@Override
	public boolean put (JsonObject spec, Callback callback, ApiStreamSource... attachments) {
		return request (ApiVerb.PUT, spec, callback);
	}

	@Override
	public boolean delete (JsonObject spec, Callback callback) {
		return request (ApiVerb.DELETE, spec, callback);
	}

	@Override
	public boolean head (JsonObject spec, Callback callback) {
		return request (ApiVerb.HEAD, spec, callback);
	}

	@Override
	public boolean patch (JsonObject spec, Callback callback) {
		return request (ApiVerb.PATCH, spec, callback);
	}
	
	private boolean request (ApiVerb verb, JsonObject spec, Callback callback, ApiStreamSource... attachments) {
		
		JsonObject rdata = Json.getObject (spec, Spec.Data);
		
		if (!Json.isNullOrEmpty (featureSpec)) {
			JsonObject master = featureSpec.duplicate ();
			
			Json.resolve (master, ECompiler, new VariableResolver () {
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
				Json.resolve (headers, ECompiler, new VariableResolver () {
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
						return false;
					}
				}
			} else if (contentType.startsWith (ContentTypes.Json)) {
				body = RequestBody.create (mediaType, rdata == null ? JsonObject.EMPTY_OBJECT : rdata.toString ());
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
				return false;
			} else {
				callback.onDone (
					response.code (), 
					serializer.serialize (response.body ().byteStream ())
				);
				return true;
			}
			
		} catch (UnknownHostException uhex) {
			try {
				callback.onError (Error.UnknownHost, "Endpoint " + endpoint + " can't be resolved. Check your internet connection and make sure the endpoint is correct");
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
			return false;
		} catch (SocketTimeoutException stoex) {
			try {
				callback.onError (Error.Timeout, "Endpoint " + endpoint + " was found but " + stoex.getMessage ());
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
			return false;
		} catch (Exception ex) {
			try {
				callback.onError (Error.Other, Lang.toError (ex));
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
			return false;
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
				throw new Exception ("basic-auth password not found in spec");
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
	public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
	}

	@Override
	public Object get () {
		return null;
	}

	@Override
	public void recycle () {
	}

}

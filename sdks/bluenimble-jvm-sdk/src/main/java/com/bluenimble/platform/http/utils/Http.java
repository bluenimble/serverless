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
package com.bluenimble.platform.http.utils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.http.HttpClient;
import com.bluenimble.platform.http.HttpClientException;
import com.bluenimble.platform.http.HttpEndpoint;
import com.bluenimble.platform.http.HttpHeader;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpMethods;
import com.bluenimble.platform.http.HttpParameter;
import com.bluenimble.platform.http.impls.DefaultHttpClient;
import com.bluenimble.platform.http.impls.HttpHeaderImpl;
import com.bluenimble.platform.http.impls.HttpMessageBodyImpl;
import com.bluenimble.platform.http.impls.HttpParameterImpl;
import com.bluenimble.platform.http.impls.InputStreamHttpMessageBodyPart;
import com.bluenimble.platform.http.impls.StringHttpMessageBodyPart;
import com.bluenimble.platform.http.request.HttpRequest;
import com.bluenimble.platform.http.request.HttpRequestVisitor;
import com.bluenimble.platform.http.request.impls.AbstractHttpRequest;
import com.bluenimble.platform.http.request.impls.DeleteRequest;
import com.bluenimble.platform.http.request.impls.GetRequest;
import com.bluenimble.platform.http.request.impls.HeadRequest;
import com.bluenimble.platform.http.request.impls.PostRequest;
import com.bluenimble.platform.http.request.impls.PutRequest;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.json.JsonObject;

public class Http {
	
	public interface Spec {
		String Verb 	= "verb";
		String Url 		= "url";
		String Data 	= "data";
		String Headers 	= "headers";
		interface proxy {
			String Type 	= "type";
			String Endpoint = "endpoint";
			String Port 	= "port";
			interface auth 	{
				String user 	= "user";
				String password = "password";
			}
		}
		interface timeouts {
			String Connect 	= "connect";
			String Read 	= "read";
		}
	}
	
	private static HttpClient Client; 
	static {
		try {
			Client = new DefaultHttpClient ();
		} catch (Exception e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}
	
	private static final Map<String, Class<? extends AbstractHttpRequest>> Requests = new HashMap<String, Class<? extends AbstractHttpRequest>> ();
	static {
		Requests.put (HttpMethods.GET, GetRequest.class);
		Requests.put (HttpMethods.POST, PostRequest.class);
		Requests.put (HttpMethods.PUT, PutRequest.class);
		Requests.put (HttpMethods.DELETE, DeleteRequest.class);
		Requests.put (HttpMethods.HEAD, HeadRequest.class);
	}
	public static HttpResponse get (JsonObject spec, HttpRequestVisitor visitor) throws HttpClientException {
		return send (request (HttpMethods.GET, spec, visitor));
	}

	public static HttpResponse post (JsonObject spec, HttpRequestVisitor visitor) throws HttpClientException {
		return send (request (HttpMethods.POST, spec, visitor));
	}

	public static HttpResponse put (JsonObject spec, HttpRequestVisitor visitor) throws HttpClientException {
		return send (request (HttpMethods.PUT, spec, visitor));
	}

	public static HttpResponse delete (JsonObject spec, HttpRequestVisitor visitor) throws HttpClientException {
		return send (request (HttpMethods.DELETE, spec, visitor));
	}

	public static HttpResponse head (JsonObject spec, HttpRequestVisitor visitor) throws HttpClientException {
		return send (request (HttpMethods.HEAD, spec, visitor));
	}

	public static HttpRequest request (JsonObject spec, HttpRequestVisitor visitor) throws HttpClientException {
		return request (Json.getString (spec, Spec.Verb, HttpMethods.GET), spec, visitor);
	}

	private static HttpRequest request (String verb, JsonObject spec, HttpRequestVisitor visitor) throws HttpClientException {
		verb = verb.toUpperCase ();
		Class<? extends AbstractHttpRequest> requestClass = Requests.get (verb);
		if (requestClass == null) {
			throw new HttpClientException ("unsupported http method " + verb);
		}
		
		String url = Json.getString (spec, Spec.Url);
		
		AbstractHttpRequest request;
		try {
			request = requestClass.getConstructor (new Class [] { HttpEndpoint.class })
					.newInstance (
						HttpUtils.createEndpoint (new URI (url))
					);
		} catch (Exception ex) {
			throw new HttpClientException (ex.getMessage (), ex);
		}
		
		request.setVisitor (visitor);
		
		// add timeouts
		JsonObject oTimeouts = Json.getObject (spec, Spec.timeouts.class.getSimpleName ());
		if (oTimeouts != null && !oTimeouts.isEmpty ()) {
			int connectTimeout = Json.getInteger (oTimeouts, Spec.timeouts.Connect, 0);
			if (connectTimeout > 100) {
				request.setConnectTimeout (connectTimeout);
			}
			int readTimeout = Json.getInteger (oTimeouts, Spec.timeouts.Read, 0);
			if (readTimeout > 100) {
				request.setReadTimeout (readTimeout);
			}
		}
		// add proxy
		JsonObject oProxy = Json.getObject (spec, Spec.proxy.class.getSimpleName ());
		if (oProxy != null && !oProxy.isEmpty ()) {
			Proxy.Type proxyType = Proxy.Type.valueOf (Json.getString (oProxy, Spec.proxy.Type, Proxy.Type.HTTP.name ()).toUpperCase ());
			
			int proxyPort = Json.getInteger (oProxy, Spec.proxy.Port, 8080);

			Proxy proxy = new Proxy (
				proxyType,
				new InetSocketAddress (Json.getString (oProxy, Spec.proxy.Endpoint), proxyPort)
			);
			
			request.setProxy (proxy);
			
		}
		
		String contentType = null;
		
		// add headers
		JsonObject headers = Json.getObject (spec, Spec.Headers);
		if (headers != null && !headers.isEmpty ()) {
			List<HttpHeader> hHeaders = new ArrayList<HttpHeader> ();
			request.setHeaders (hHeaders);
			Iterator<String> keys = headers.keys ();
			while (keys.hasNext ()) {
				String h = keys.next ();
				String hv = String.valueOf (headers.get (h));
				if (HttpHeaders.CONTENT_TYPE.toUpperCase ().equals (h.toUpperCase ())) {
					contentType = hv;
				} else {
					hHeaders.add (new HttpHeaderImpl (h, hv));
				}
			}
			headers.remove (HttpHeaders.CONTENT_TYPE);
		}
		
		request.setContentType (contentType);
		
		// add params
		JsonObject data = Json.getObject (spec, Spec.Data);
		if (data != null && !data.isEmpty ()) {
			if (ContentTypes.Json.equals (contentType)) {
				HttpMessageBody body = new HttpMessageBodyImpl ();
				request.setBody (body);
				body.add (new StringHttpMessageBodyPart (data.toString ()));
			} else {
				List<HttpParameter> hParams = new ArrayList<HttpParameter> ();
				request.setParameters (hParams);
				Iterator<String> keys = data.keys ();
				while (keys.hasNext ()) {
					String p = keys.next ();
					hParams.add (new HttpParameterImpl (p, data.get (p)));
				}
			}
		}
		
		return request;
		
	}

	public static HttpResponse send (HttpRequest request) throws HttpClientException {
		return Client.send (request);
	}
	
	public static void addStream (HttpRequest request, String name, ApiStreamSource ss) throws HttpClientException {
		HttpMessageBody body = new HttpMessageBodyImpl ();
		request.setBody (body);
		body.add (new InputStreamHttpMessageBodyPart (name == null ? ss.name () : name, ss.name (), ss.stream ()));
	}

}

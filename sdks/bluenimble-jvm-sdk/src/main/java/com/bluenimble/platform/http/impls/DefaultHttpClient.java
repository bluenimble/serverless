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
package com.bluenimble.platform.http.impls;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.http.HttpClient;
import com.bluenimble.platform.http.HttpClientException;
import com.bluenimble.platform.http.HttpHeader;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.HttpMethods;
import com.bluenimble.platform.http.request.HttpRequest;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.http.response.impls.HttpResponseImpl;

public class DefaultHttpClient implements HttpClient {

	private static final long serialVersionUID = 8918611377786119408L;
	
	private static final String GZip = "gzip";
	
	static {
		System.setProperty ("http.agent", "");
	} 
	
	private HostnameVerifier TrustAllHostVerifier = new HostnameVerifier () {
		@Override
		public boolean verify (String host, SSLSession session) {
			if (!host.equalsIgnoreCase (session.getPeerHost ())) {
                System.out.println ("Warning: URL host '" + host + "' is different than SSLSession host '" + session.getPeerHost () + "'.");
            }
			return true;
		}
	};
	
	private static SSLSocketFactory TrustAllSocketFactory;
	static {
		TrustManager [] trustAllCerts = new TrustManager[] { new X509TrustManager () {
            public X509Certificate [] getAcceptedIssuers() { return null; }
            public void checkClientTrusted (X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }

        } };
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance ("TLS");
			sslContext.init (null, trustAllCerts, null);
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
        TrustAllSocketFactory = sslContext.getSocketFactory ();
	}
	
	protected String [] cookies;
	
	protected boolean updateCookies;
	
	protected boolean trustAll;
	
	public HttpResponse send (final HttpRequest request) throws HttpClientException {

		HttpURLConnection hc = null;
		
		try {
			if (request == null || request.getURI () == null) {
				throw new HttpClientException("No request to proceed");
			}

			URL url = request.getURI ().toURL ();
			
			URLConnection connection = null;
			if (request.getProxy () != null) {
				connection = url.openConnection (request.getProxy ());
			} else {
				connection = url.openConnection ();
			}
			
			hc = (HttpURLConnection) connection;
			
			if (hc instanceof HttpsURLConnection) {
				
				HttpsURLConnection https = (HttpsURLConnection)hc;
				
				if (trustAll) {
			        https.setSSLSocketFactory (TrustAllSocketFactory);
					https.setHostnameVerifier (TrustAllHostVerifier);
				}
				
			}

			hc.setConnectTimeout 	(request.getConnectTimeout ());
			hc.setReadTimeout 		(request.getReadTimeout ());
			
			hc.setRequestMethod (request.getName ());

			if (request.getName ().equals (HttpMethods.POST) || request.getName ().equals (HttpMethods.PUT)) {
				connection.setDoOutput (true);
			}

			if (!(connection instanceof HttpURLConnection)) {
				throw new HttpClientException ("Only Http request can be handled");
			}
			
			setRequestCookie (request);
			
			request.write (hc);
			
			InputStream iobody = null;
			
			int status = hc.getResponseCode ();
			
			HttpResponseImpl response = new HttpResponseImpl (request.getId ());
			response.setStatus (status);

			addResponseHeaders (response, hc);
			
			String charset = request.getCharset ();
			
			HttpHeader cth = response.getHeader (HttpHeaders.CONTENT_TYPE);
			if (cth != null) {
				String [] values = cth.getValues ();
				if (values != null && values.length > 0) {
					String contentType = values [0];
					response.setContentType (contentType);
					for (String param : contentType.replace (" ", "").split(";")) {
					    if (param.startsWith ("charset=")) {
					        charset = param.split("=", 2)[1];
					        break;
					    }
					}
				} 
			}
			response.setCharset (charset);
			
			if (request.getSuccessCodes ().contains (String.valueOf (status))) {
				iobody = hc.getInputStream ();
			} else {
				iobody = hc.getErrorStream ();
			}
			
			if (GZip.equals (hc.getContentEncoding ())) {
				iobody = new GZIPInputStream (iobody);
			} 
			
			response.setBody (new HttpMessageBodyImpl (new InputStreamHttpMessageBodyPart (iobody)));
			
			updateCookies (response);

			return response;
			
		} catch (Throwable th) {
			throw new HttpClientException (th);
		} 

	}
	
	protected void onCreateConnection (HttpRequest request, HttpURLConnection hc) {
		
	}

	protected void setRequestCookie (HttpRequest request) {
		if (cookies == null) {
			return;
		}
		if (request.getHeaders () == null) {
			request.setHeaders (new ArrayList<HttpHeader> ());
		}
		List<String> cookieValues = new ArrayList<String> ();
		for (String cookie : cookies) {
			cookieValues.add (cookie.split (Lang.SEMICOLON, 2)[0]);
		}
		HttpHeader cookieHeader = new HttpHeaderImpl (HttpHeaders.COOKIE, cookieValues);
		request.getHeaders ().add (cookieHeader);
	}
	
	protected void updateCookies (HttpResponse response) {
		if (!updateCookies) {
			return;
		}
		HttpHeader cookie = response.getHeader (HttpHeaders.SET_COOKIE);
		if (cookie == null) {
			return;
		}
		cookies = cookie.getValues ();
	}
	
	protected void addResponseHeaders (HttpResponseImpl response, HttpURLConnection hc) {
		Map<String, List<String>> httpHeaders = hc.getHeaderFields ();
		if (httpHeaders == null || httpHeaders.isEmpty ()) {
			return;
		}
		List<HttpHeader> headers = new ArrayList<HttpHeader> ();
		for (Entry<String, List<String>> header : httpHeaders.entrySet ()) {
			headers.add (new HttpHeaderImpl (header.getKey (), header.getValue ()));
		}
		response.setHeaders (headers);
	} 

	public boolean isTrustAll () {
		return trustAll;
	}

	public void setTrustAll (boolean trustAll) {
		this.trustAll = trustAll;
	}

}

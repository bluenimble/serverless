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
package com.bluenimble.platform.remote.impls.http.bnb;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.utils.DataSigner;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.impls.http.RequestParameter;

import okhttp3.Request;

public class AccessSecretKeysBasedHttpRequestSigner implements Serializable {
	
	private static final long serialVersionUID = 5811507387228614019L;

	protected static final Comparator<RequestParameter> PARAMETER_COMPARATOR = new Comparator<RequestParameter> () {
		public int compare (RequestParameter p1, RequestParameter p2) {
			return p1.name.compareToIgnoreCase (p2.name);
		}
	}; 
	
	public enum PlaceHolder {
		parameter,
		header
	}
	
	private static final String DefaultPayload 	= "m>h>p>d>k>t";
	private static final String DefaultScheme 	= "Bearer";
	
	private static final String DefaultSignatureAlgorithm 	= DataSigner.DEFAULT_SIGN_ALGORITHM;
	private static final String DefaultHashingAlgorithm 	= DataSigner.DEFAULT_HASH_ALGORITHM;

	//private static final String DefaultSignatureName 		= "Signature";
	//private static final PlaceHolder 		
	//							DefaultPlaceHolder 			= PlaceHolder.header;

	public Request sign (Request request, List<RequestParameter> params, JsonObject spec) throws Exception {
		
		JsonObject replace = Json.getObject (spec, Remote.Spec.SignReplace);
		
		String timestamp = Lang.utc ();
		
		String signature = null;
		try {
			URI uri = request.url ().uri ();
			signature = generate (params, spec, timestamp, resolve (uri.getScheme () + "://" + uri.getHost (), replace), uri.getPort (), resolve (uri.getPath (), replace), request.method ());
		} catch (Throwable th) {
			throw new Exception (th);
		}
		
		// placeholder
		PlaceHolder placeholder = PlaceHolder.header;
		try {
			placeholder = PlaceHolder.valueOf (Json.getString (spec, Remote.Spec.SignPlaceholder, PlaceHolder.header.name ()));
		} catch (Exception ex) { /* Ignore */ }
		
		if (placeholder.equals (PlaceHolder.header)) {
	    	return request.newBuilder ().header (
	    			HttpHeaders.AUTHORIZATION, 
	    			Json.getString (spec, Remote.Spec.SignScheme, DefaultScheme) + " " + Json.getString (spec, Remote.Spec.SignKey) + ":" + signature
	        	).header (ApiHeaders.Timestamp, timestamp).build ();
		} 
		
		return request;
    	
	}
	
	private String resolve (String path, JsonObject tokens) {
		if (Json.isNullOrEmpty (tokens)) {
			return path;
		}
		Iterator<String> keys = tokens.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			path = Lang.replace (path, key, tokens.getString (key));
		}
		return path;
	}

	private String generate (List<RequestParameter> params, JsonObject oSpec, String timestamp, String host, int port, String path, String verb) throws Exception {
		
		String sParams = buildParameters (params);
		
		StringBuilder sb = new StringBuilder ();
		
		String accessKey = Json.getString (oSpec, Remote.Spec.SignKey);
		String secretKey = Json.getString (oSpec, Remote.Spec.SignSecret);
		
		String payload = Json.getString (oSpec, Remote.Spec.SignPayload, DefaultPayload);
		
		JsonObject data = Json.getObject (oSpec, Remote.Spec.SignData);

		if (payload != null) {
			char[] chars = payload.toCharArray ();
			for (int i = 0; i < chars.length; i++) {
				if (chars[i] == 'm') {
					sb.append (verb);
				} else if (chars[i] == 'h') {
					sb.append (host);
					if (port > 0) {
						sb.append (Lang.COLON).append (String.valueOf (port));
					}
				} else if (chars[i] == 'p') {
					sb.append (path);
				} else if (chars[i] == 'd') {
					sb.append (sParams);
				} else if (chars[i] == 'k') {
					sb.append (accessKey);
				} else if (chars[i] == '>') {
					sb.append (Lang.ENDLN);
				} else if (chars[i] == 't') {
					sb.append (timestamp);
				} else {
					String d = Json.getString (data, String.valueOf (chars[i]));
					if (d != null) {
						sb.append (d);
					} else {
						sb.append (String.valueOf (chars[i]));
					}
				}
			}
		}

		String toSign = sb.toString ();
		sb.setLength (0);
		sb = null;
		
		System.out.println ("====SING====");
		System.out.println (toSign);
		System.out.println ("====SING====");
		
        byte [] bsecret = (Lang.isNullOrEmpty (secretKey) ? null : secretKey.getBytes (DataSigner.UTF8));
        
        String signature = DataSigner.sign (
            toSign.getBytes (DataSigner.UTF8), 
            bsecret, 
            Json.getString (oSpec, Remote.Spec.SignAlgorithm, DefaultSignatureAlgorithm),
            Json.getString (oSpec, Remote.Spec.SignHashing, DefaultHashingAlgorithm),
            DataSigner.UTF8
        );

		return signature;
		
	}

	private String buildParameters (List<RequestParameter> params) {
		
		String sParams = Lang.BLANK;
		if (params == null || params.isEmpty ()) {
			return sParams;
		}
		
		StringBuilder sb = new StringBuilder ();

		Collections.sort (params, PARAMETER_COMPARATOR);
		
		// add parameters
		for (int i = 0; i < params.size (); i++) {
			RequestParameter p = params.get(i);
			sb.append (encode (p.name)).append (Lang.EQUALS);
			if (p.value != null) {
				sb.append (encode (String.valueOf (p.value)));
			}
			if (i < (params.size() - 1)) {
				sb.append (Lang.AMP);
			}
		}

		sParams = sb.toString ();
		
		sb.setLength (0);
		
		return sParams;

	}

	private String encode (String s) {
		String out;
		try {
			out = URLEncoder.encode(s, DataSigner.UTF8).replace ("+", "%20")
					.replace (Lang.STAR, "%2A").replace("%7E", "~");
		} catch (UnsupportedEncodingException e) {
			out = s;
		}
		return out;
	}
	
}

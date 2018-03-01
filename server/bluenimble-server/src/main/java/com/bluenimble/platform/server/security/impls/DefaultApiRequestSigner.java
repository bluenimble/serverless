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
package com.bluenimble.platform.server.security.impls;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.security.ApiRequestSigner;
import com.bluenimble.platform.api.security.ApiRequestSignerException;
import com.bluenimble.platform.encoding.Base64;

public class DefaultApiRequestSigner implements ApiRequestSigner {
	
	public static final 	String HEXA_ALGORITHM         	= "hexa";
    public static final 	String BASE64_ALGORITHM       	= "base64";
    
	private static final 	String HMAC_SHA256_ALGORITHM 	= "HmacSHA256";
	
	/**
	 * 
	 * [VERB] + '\n' + [ENDPOINT] + '\n' + [REQUEST URI: must starts with '/'] + '\n' + [ACCESS_KEY] '\n' + [ORDERED REQUEST PARAMETERS] + '\n' + [Timestamp Header]
	 * 
	 **/
	public String sign (ApiRequest request, String utcTimestamp, String accessKey, String secretKey, boolean writeTorequest) throws ApiRequestSignerException {
		
		SortedMap<String, String> sorted = new TreeMap<String, String> (String.CASE_INSENSITIVE_ORDER);
		
		Iterator<String> names = request.keys (Scope.Parameter);
		if (names != null) {
			while (names.hasNext ()) {
				String name = names.next ();
				sorted.put (name, String.valueOf (request.get (name)));
			}
		}
		
		String params = canonicalize (sorted);
		
		String rEndpoint = request.getEndpoint ();
		
		StringBuilder sb = new StringBuilder ();
		sb	.append (request.getVerb ().name ()).append (Lang.ENDLN)
			.append (request.getScheme ()).append (Lang.COLON).append (Lang.SLASH).append (Lang.SLASH).append (rEndpoint).append (Lang.ENDLN)
			.append (request.getPath ()).append (Lang.ENDLN)
			
			.append (params).append (Lang.ENDLN)
			.append (accessKey).append (Lang.ENDLN)
			.append (utcTimestamp);
		
		String s = sb.toString ();
		sb.setLength (0);
		
		try {
			s = hmac (secretKey, s);
		} catch (Exception e) {
			throw new ApiRequestSignerException (e.getMessage (), e);
		}
		
		return s;
		
	}

	private static String hmac (String secretKey, String data) throws Exception {
	    
		byte[] secretKeyBytes = secretKey.getBytes (Encodings.UTF8);
		
		SecretKeySpec secretKeySpec =
	      new SecretKeySpec (secretKeyBytes, HMAC_SHA256_ALGORITHM);
		
		Mac mac = Mac.getInstance (HMAC_SHA256_ALGORITHM);
	    mac.init (secretKeySpec);
	    
		String signature = null;
		byte [] bytes;
		byte [] rawHmac;
		try {
			bytes = data.getBytes (Encodings.UTF8);
			rawHmac = mac.doFinal (bytes);
			signature = new String (Base64.encodeBase64 (rawHmac), Encodings.UTF8).trim ();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException (Encodings.UTF8 + " is unsupported!", e);
		}
		return signature;
	}
	
	private static String canonicalize (SortedMap<String, String> map) {
		if (map.isEmpty ()) {
			return Lang.BLANK;
		}

		StringBuilder sb = new StringBuilder ();
		
		Iterator<Map.Entry<String, String>> iter = map.entrySet ().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, String> kvpair = iter.next();
			sb.append (encode (kvpair.getKey ()));
			sb.append (Lang.EQUALS);
			sb.append (encode (kvpair.getValue ()));
			if (iter.hasNext()) {
				sb.append (Lang.AMP);
			}
		}
		
		String canonical = sb.toString ();
		sb.setLength (0);
		
		return canonical;
	}

	private static String encode (String s) {
		String out;
		try {
			out = URLEncoder.encode(s, Encodings.UTF8).replace (Lang.PLUS, "%20")
					.replace (Lang.STAR, "%2A").replace("%7E", "~");
		} catch (UnsupportedEncodingException e) {
			out = s;
		}
		return out;
	}
    
}
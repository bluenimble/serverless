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
package com.bluenimble.platform.http.auth.impls;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.http.HttpHeader;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.HttpParameter;
import com.bluenimble.platform.http.auth.HttpRequestSigner;
import com.bluenimble.platform.http.auth.HttpRequestSignerException;
import com.bluenimble.platform.http.impls.HttpHeaderImpl;
import com.bluenimble.platform.http.impls.HttpParameterImpl;
import com.bluenimble.platform.http.request.HttpRequest;
import com.bluenimble.platform.http.utils.DataSigner;

public class AccessSecretKeysBasedHttpRequestSigner implements HttpRequestSigner {
	
	private static final long serialVersionUID = 5811507387228614019L;

	protected static final Comparator<HttpParameter> PARAMETER_COMPARATOR = new Comparator<HttpParameter> () {
		public int compare (HttpParameter p1, HttpParameter p2) {
			return p1.getName ().compareToIgnoreCase (p2.getName ());
		}
	}; 
	
	public enum SignaturePlaceHolder {
		Parameters,
		Headers
	}
	
	protected 			String 						spec = "d";
	protected 			String 						accessKey;
	protected transient String 						secretKey;
	
	protected 			String 						encoding = DataSigner.UTF8;
	
	protected 			String 						signatureAlgorithm = DataSigner.DEFAULT_SIGN_ALGORITHM;
	protected 			String 						hashingAlgorithm = DataSigner.DEFAULT_HASH_ALGORITHM;

	protected 			String 						scheme = "Bearer";
	protected 			String 						signatureParameterName;
	protected 			SignaturePlaceHolder 		signatureParameterPlace = SignaturePlaceHolder.Headers;
	
	protected 			Map<Character, String>		data = new HashMap<Character, String> ();
	
	public Map<Character, String> getData() {
		return data;
	}

	public void setData(Map<Character, String> data) {
		this.data = data;
	}

	public AccessSecretKeysBasedHttpRequestSigner (String spec, String scheme, String accessKey, String secretKey) {
		this.spec = spec;
		this.scheme = scheme;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	@Override
	public void sign (HttpRequest request)
			throws HttpRequestSignerException {
		
		List<HttpParameter> params = request.getParameters ();
		
		String signature = null;
		try {
			URI uri = request.getURI ();
			signature = generate (params, uri.getScheme () + "://" + uri.getHost (), uri.getPort (), uri.getPath (), request.getName ());
		} catch (Throwable th) {
			throw new HttpRequestSignerException (th);
		}
		
        if (SignaturePlaceHolder.Headers.equals (signatureParameterPlace)) {
        	List<HttpHeader> headers = request.getHeaders ();
    		
    		if (headers == null) {
    			headers = new ArrayList<HttpHeader> ();
    			request.setHeaders (headers);
    		}
    		
    		headers.add (
				new HttpHeaderImpl (
					HttpHeaders.AUTHORIZATION, 
					scheme + " " + accessKey + ":" + signature
				)
			);
        } else {
            if (signatureParameterName == null) {
            	signatureParameterName = "signature";
            }
            params.add (new HttpParameterImpl (signatureParameterName, encode (signature)));
        }
	}
	
	public String getSpec () {
		return spec;
	}

	public void setSpec (String spec) {
		this.spec = spec;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getSignatureAlgorithm() {
		return signatureAlgorithm;
	}

	public void setSignatureAlgorithm(String signatureAlgorithm) {
		this.signatureAlgorithm = signatureAlgorithm;
	}

	public String getHashingAlgorithm() {
		return hashingAlgorithm;
	}

	public void setHashingAlgorithm(String hashingAlgorithm) {
		this.hashingAlgorithm = hashingAlgorithm;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getSignatureParameterName() {
		return signatureParameterName;
	}

	public void setSignatureParameterName(String signatureParameterName) {
		this.signatureParameterName = signatureParameterName;
	}

	public SignaturePlaceHolder getSignatureParameterPlace() {
		return signatureParameterPlace;
	}

	public void setSignatureParameterPlace(
			SignaturePlaceHolder signatureParameterPlace) {
		this.signatureParameterPlace = signatureParameterPlace;
	}

	private String generate (List<HttpParameter> params, String host, int port, String path, String verb) throws Exception {
		
		String sParams = buildParameters (params);
		
		StringBuilder sb = new StringBuilder ();

		if (spec != null) {
			char[] chars = spec.toCharArray ();
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
				} else {
					String d = data.get (chars[i]);
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
		
		//System.out.println ("====SING====");
		//System.out.println (toSign);
		//System.out.println ("====SING====");
		
        // generate signature
        if (encoding == null) {
            encoding = DataSigner.UTF8;
        }
        
        byte [] bsecret = (secretKey == null ? null : secretKey.getBytes (encoding));
        
        String signature = DataSigner.sign (
            toSign.getBytes (encoding), 
            bsecret, 
            signatureAlgorithm,
            hashingAlgorithm,
            encoding
        );
		
		//System.out.println ("Calculated: " + signature);

		return signature;
		
	}

	private String buildParameters (List<HttpParameter> params) {
		
		String sParams = Lang.BLANK;
		if (params == null || params.isEmpty ()) {
			return sParams;
		}
		
		StringBuilder sb = new StringBuilder ();

		Collections.sort (params, PARAMETER_COMPARATOR);
		
		// add parameters
		for (int i = 0; i < params.size (); i++) {
			HttpParameter p = params.get(i);
			sb.append (encode (p.getName ())).append (Lang.EQUALS);
			if (p.getValue () != null) {
				sb.append (encode (String.valueOf (p.getValue ())));
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
			out = URLEncoder.encode(s, encoding).replace ("+", "%20")
					.replace (Lang.STAR, "%2A").replace("%7E", "~");
		} catch (UnsupportedEncodingException e) {
			out = s;
		}
		return out;
	}
	
}

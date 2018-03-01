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

import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.http.HttpHeader;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.auth.HttpRequestSigner;
import com.bluenimble.platform.http.auth.HttpRequestSignerException;
import com.bluenimble.platform.http.impls.HttpHeaderImpl;
import com.bluenimble.platform.http.request.HttpRequest;

public class KeyHttpRequestSigner implements HttpRequestSigner {

	private static final long serialVersionUID = -8708366425959546343L;
	
	private static final String DEFAULT_TYPE = "Bearer";
	
	private static final Base64 BASE64 = new Base64 ();
	
	protected 			String 	type;
	protected transient String 	key;
	protected 			boolean	encode;
	
	public KeyHttpRequestSigner (String type, String key, boolean encode) {
		setType (type);
		this.key = key;
		this.encode = encode;
	}

	public KeyHttpRequestSigner (String type, String key) {
		this (type, key, false);
	}

	public KeyHttpRequestSigner (String key) {
		this (null, key);
	}

	@Override
	public void sign (HttpRequest request) throws HttpRequestSignerException {
		
		List<HttpHeader> headers = request.getHeaders ();
		
		if (headers == null) {
			headers = new ArrayList<HttpHeader> ();
			request.setHeaders (headers);
		}
		
		headers.add (
			new HttpHeaderImpl (
				HttpHeaders.AUTHORIZATION, 
				type + " " + (encode ? new String (BASE64.encode (key.getBytes ())).replaceAll ("\n", "") : key)
			)
		);
		
	}

	public String getType () {
		return type;
	}

	public void setType (String type) {
		if (Lang.isNullOrEmpty (type)) {
			type = DEFAULT_TYPE;
		}
		this.type = type;
	}

	public String getKey () {
		return key;
	}

	public void setKey (String key) {
		this.key = key;
	}

	public boolean isEncode() {
		return encode;
	}

	public void setEncode(boolean encode) {
		this.encode = encode;
	}
	
	
	
}

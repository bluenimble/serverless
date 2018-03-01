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
package com.bluenimble.platform.api.impls.scripting.libraries;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.http.HttpHeader;
import com.bluenimble.platform.http.auth.HttpRequestSignerException;
import com.bluenimble.platform.http.auth.impls.AccessSecretKeysBasedHttpRequestSigner;
import com.bluenimble.platform.http.impls.HttpHeaderImpl;
import com.bluenimble.platform.http.request.HttpRequest;
import com.bluenimble.platform.http.request.HttpRequestVisitor;
import com.bluenimble.platform.http.request.HttpRequestWriteException;

public class BlueNimbleHttpRequestVisitor implements HttpRequestVisitor {

	private static final long serialVersionUID = -211427519182758650L;

	private String accessKey;
	private String secretKey;
	
	public BlueNimbleHttpRequestVisitor (String accessKey, String secretKey) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}
	
	@Override
	public void visit (HttpRequest request, HttpURLConnection connection) throws HttpRequestWriteException {
		// sign request
		
		List<HttpHeader> headers = request.getHeaders ();
		if (headers == null) {
			headers = new ArrayList<HttpHeader> ();
			request.setHeaders (headers);
		}

		AccessSecretKeysBasedHttpRequestSigner signer = 
				new AccessSecretKeysBasedHttpRequestSigner ("m>h>p>d>k>t", "Bearer", accessKey, secretKey);
		
		String timestamp = Lang.utc ();
		
		headers.add (new HttpHeaderImpl (ApiHeaders.Timestamp, timestamp));
		
		signer.getData ().put ('t', timestamp);
		
		try {
			signer.sign (request);
		} catch (HttpRequestSignerException e) {
			throw new HttpRequestWriteException (e.getMessage (), e);
		}
		
	}

}

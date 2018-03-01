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
import java.util.List;

import com.bluenimble.platform.http.HttpEndpoint;
import com.bluenimble.platform.http.HttpParameter;
import com.bluenimble.platform.http.request.HttpRequest;
import com.bluenimble.platform.http.request.HttpRequestVisitor;
import com.bluenimble.platform.http.request.HttpRequestWriteException;
import com.bluenimble.platform.http.request.impls.AbstractHttpRequest;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.http.HttpParameters;

public class OAuthHttpRequestVisitor implements HttpRequestVisitor {

	private static final long serialVersionUID = -211427519182758650L;

	private String key;
	private String secret;
	
	public OAuthHttpRequestVisitor (String key, String secret) {
		this.key = key;
		this.secret = secret;
	}
	
	@Override
	public void visit (HttpRequest request, HttpURLConnection connection) throws HttpRequestWriteException {
		HttpEndpoint endpoint = ((AbstractHttpRequest)request).getEndpoint ();
		
		OAuthConsumer consumer = new DefaultOAuthConsumer (key, secret);
        HttpParameters encodedParams =  new HttpParameters ();
        
        List<HttpParameter> params = request.getParameters ();
        if (params != null && !params.isEmpty ()) {
        	for (HttpParameter p : params) {
        		if (p.getValue () != null) {
                    encodedParams.put (p.getName (), OAuth.percentEncode (String.valueOf (p.getValue ())));
        		}
        	}
        }
        encodedParams.put ("realm", endpoint.getScheme () + "://" + endpoint.getHost () + endpoint.getPath ());
        
        consumer.setAdditionalParameters (encodedParams);
                
        try {
			consumer.sign (connection);
		} catch (Exception e) {
			throw new HttpRequestWriteException (e.getMessage (), e);
		}
	}

}

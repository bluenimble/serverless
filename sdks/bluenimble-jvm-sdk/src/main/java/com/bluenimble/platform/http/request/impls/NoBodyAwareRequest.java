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
package com.bluenimble.platform.http.request.impls;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.http.HttpEndpoint;
import com.bluenimble.platform.http.HttpParameter;
import com.bluenimble.platform.http.request.HttpRequestWriteException;
import com.bluenimble.platform.http.utils.HttpUtils;

public abstract class NoBodyAwareRequest extends AbstractHttpRequest {

	private static final long serialVersionUID = 6156567388966926923L;

	protected NoBodyAwareRequest (String method, HttpEndpoint endpoint) {
		super (method, endpoint);
		if (!Lang.isNullOrEmpty (endpoint.getQuery ())) {
			parameters = new ArrayList<HttpParameter> ();
			HttpUtils.parseParameters (endpoint.getQuery (), parameters);
			setParameters (parameters);
			endpoint.setQuery (null);
		}
	}
	
	@Override
	public void write (HttpURLConnection hc) throws HttpRequestWriteException {
		if (visitor != null) {
			visitor.visit (this, hc);
		}
		addHeaders (hc);
		// nothing going to the body
	}
	
	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder ();
		try {
			sb.append (name).append (" ").append (getURI ()).append ("\n");
		} catch (Exception e) {
			
		}
		sb.append (super.toString ());
		String s = sb.toString ();
		sb.setLength (0);
		sb = null;
		return s;
	}
	
}

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
package com.bluenimble.platform.http;

import java.io.Serializable;

public class HttpEndpoint implements Serializable {
	
	private static final long serialVersionUID = 8352062844479208019L;

	public static final String HTTP_SCHEME = "http";
	public static final String HTTPS_SCHEME = "https";
	
	protected String 	scheme = "http";
	protected String 	host;
	protected int 		port;
	protected String 	path;
	protected String 	query;
	
	public HttpEndpoint (String scheme, String host, int port, String path, String query) {
		if (scheme == null) {
			scheme = HTTP_SCHEME;
		}
		this.host = host;
		this.port = port;
		this.scheme = scheme;
		this.path = path;
		this.query = query;
	}
	
	public HttpEndpoint (String scheme, String host, String query) {
		this (scheme, host, 0, null, query);
	}
	
	public HttpEndpoint (String scheme, String host, int port, String path) {
		this (scheme, host, port, path, null);
	}
	
	public HttpEndpoint (String scheme, String host, int port) {
		this (scheme, host, port, null);
	}
	
	public HttpEndpoint (String scheme, String host) {
		this (scheme, host, 0);
	}
	
	public HttpEndpoint (String host) {
		this (HTTP_SCHEME, host);
	}
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public String getScheme() {
		return scheme;
	}
	
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
	
}

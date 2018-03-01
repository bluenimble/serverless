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
package com.bluenimble.platform.http.request;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import com.bluenimble.platform.http.HttpHeader;
import com.bluenimble.platform.http.HttpMessage;
import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpParameter;

public interface HttpRequest extends HttpMessage {
	
	String 				getId ();
	String 				getName ();
	URI 				getURI () throws UnsupportedEncodingException, URISyntaxException;

	void 				setCharset (String charset);
	
	void 				setContentType (String contentType);
	String 				getContentType ();

	boolean 			isCachingEnabled ();
	void 				setCachingEnabled (boolean cachingEnabled);
	
	int					getConnectTimeout 	();
	void				setConnectTimeout 	(int timeout);
	
	int					getReadTimeout 		();
	void				setReadTimeout 		(int timeout);

	void 				setHeaders (List<HttpHeader> headers);

	List<HttpParameter>	getParameters ();
	void 				setParameters (List<HttpParameter> parameters);
	
	void				setBody (HttpMessageBody body);

	void				write (HttpURLConnection hc) throws HttpRequestWriteException;
	
	void				setSuccessCodes (Set<String> codes);
	Set<String>			getSuccessCodes ();

	boolean 			isDebugMode ();
	void 				setDebugMode (boolean debugMode);

	Proxy 				getProxy ();
	
	void 				setVisitor (HttpRequestVisitor visitor);
	HttpRequestVisitor 	getVisitor ();
	
	String []			getSniHosts ();
	void				setSniHosts (String [] hosts);

}

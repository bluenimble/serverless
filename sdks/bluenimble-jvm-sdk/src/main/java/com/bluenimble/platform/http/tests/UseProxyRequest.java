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
package com.bluenimble.platform.http.tests;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

import com.bluenimble.platform.http.impls.DefaultHttpClient;
import com.bluenimble.platform.http.request.impls.GetRequest;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.http.utils.HttpUtils;

public class UseProxyRequest {

	public static void main (String [] args) throws Exception {
		
		DefaultHttpClient client = new DefaultHttpClient ();
		
		GetRequest request = new GetRequest (HttpUtils.createEndpoint (new URI ("http://amazon.com/Apple-Retina-display-MD510LL-9-7-Inch/dp/B009W8YQ6K/ref=sr_1_1?s=pc&ie=UTF8&qid=1414360465&sr=1-1&keywords=ipad")));
		request.setProxy (new Proxy (Proxy.Type.HTTP, new InetSocketAddress ("us-il.proxymesh.com", 31280)));

		HttpResponse response = null;
		for (int i = 0; i < 50; i++) {
			response = client.send (request);
			System.out.println (response.getStatus ());
		}
		
	}
	
}

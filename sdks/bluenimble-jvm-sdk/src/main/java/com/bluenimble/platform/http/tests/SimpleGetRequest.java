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

import java.net.URI;

import com.bluenimble.platform.http.impls.DefaultHttpClient;
import com.bluenimble.platform.http.impls.HttpParameterImpl;
import com.bluenimble.platform.http.request.impls.GetRequest;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.http.utils.HttpUtils;

public class SimpleGetRequest {
	
	public static void main (String [] args) throws Exception {
		
		DefaultHttpClient client = new DefaultHttpClient ();
		
		GetRequest request = new GetRequest (HttpUtils.createEndpoint (new URI ("https://api.indix.com/v2/summary/products?countryCode=US&app_id=2a4049dd&app_key=444978937e94a8adebdcd701660da344")));
		request.getParameters ().add (new HttpParameterImpl ("q", "hello kitty"));
		
		HttpResponse 			response = client.send (request);
		response.getBody ().dump (System.out, "utf-8", null);
		
	}
	
}

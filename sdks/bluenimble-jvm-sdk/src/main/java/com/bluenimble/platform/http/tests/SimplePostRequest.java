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

import java.io.ByteArrayOutputStream;

import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.http.utils.Http;
import com.bluenimble.platform.json.JsonObject;

public class SimplePostRequest {
	
	public static void main (String [] args) throws Exception {
		
		JsonObject spec = (JsonObject)new JsonObject ().set ("url", "http://discuss.bluenimble.com/api/users")
				.set ("headers", new JsonObject ().set ("Content-Type", "application/json").set ("Authorization", "Token 3NpuOJs7BBYcUVyKswLQ1d7ETfvzTsJixSHZcFSm"))
				.set ("data", new JsonObject ().set ("data", new JsonObject ().set ("attributes", 
						new JsonObject ().set ("username", "Simo").set ("email", "loukili.mohammed@gmail.com").set ("password", "Alph@2016")
				)));
				
		
		HttpResponse response = Http.post (spec, null);
		
		System.out.println ("Status: " + response.getStatus ());
		
		HttpMessageBody body = response.getBody ();
		if (body == null || body.count () == 0) {
			throw new Exception ("response body is empty");
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		body.dump (out, "UTF-8", null);
		
		System.out.println (
			new JsonObject (new String (out.toByteArray ()))
		);	
		
	}
	
}

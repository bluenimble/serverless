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
package com.bluenimble.platform.tools.binary.tests;

import java.util.Map;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.impls.SimpleApiRequest;
import com.bluenimble.platform.tools.binary.BinaryClientFactory;
import com.bluenimble.platform.tools.binary.BinaryClientCallback;

public class Worker extends Thread {
	
	private String					id;
	private BinaryClientFactory 	factory;
	
	public Worker (BinaryClientFactory factory, String id) {
		this.factory = factory;
		this.id = id;
	}
	
	public void run () {
		/*
		factory.create ().send (
			new SimpleApiRequest ("binary", ApiVerb.POST, "bbps", "binaryserver", "/hello/teta-" + id, "AlphaOrigin", "Alpha"),
			new BinaryClientCallback () {
				@Override
				public void onStatus (ApiResponse.Status status) {
					System.out.println ("Status-" + id + ": " + status);
				}
				@Override
				public void onHeaders (Map<String, Object> headers) {
					System.out.println ("Headers-" + id + ": " + headers);
				}
				@Override
				public void onChunk (byte [] bytes) {
					System.out.println ("Chunk-" + id + ": \n" + bytes);
				}
				@Override
				public void onFinish () {
					System.out.println ("Done! " + id);
				}
			}
		);
		*/
	}
} 
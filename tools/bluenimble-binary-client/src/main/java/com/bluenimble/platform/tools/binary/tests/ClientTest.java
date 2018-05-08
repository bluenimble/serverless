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

import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.pooling.PoolConfig;
import com.bluenimble.platform.tools.binary.BinaryClientCallback;
import com.bluenimble.platform.tools.binary.BinaryClientFactory;
import com.bluenimble.platform.tools.binary.impls.netty.NettyBinaryClientFactory;

public class ClientTest {

	public static void main (String[] args) throws Exception {
		
		BinaryClientFactory factory = new NettyBinaryClientFactory (
			"localhost", 7070, 
			new PoolConfig ()
				.setPartitionSize (5)
				.setMinSize (5)
				.setMaxSize (10)
				.setMaxIdleMilliseconds(60 * 1000 * 5)
		);
		
		factory.create ().send (
			(JsonObject)new JsonObject ()
				.set (ApiRequest.Fields.Verb, "GET")
				.set (ApiRequest.Fields.Path, "/sys/mgm/instance/keys"),
			new BinaryClientCallback () {
				@Override
				public void onStatus (int status) {
					System.out.println ("Status: " + status);
				}
				@Override
				public void onHeaders (Map<String, Object> headers) {
					System.out.println ("Headers: " + headers);
				}
				@Override
				public void onChunk (byte [] bytes) {
					System.out.println ("Chunk: \n" + bytes);
				}
				@Override
				public void onFinish () {
					System.out.println ("Done! ");
				}
			}
		);
		
	}	
}

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
package com.bluenimble.platform.remote;

import java.io.Serializable;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.json.JsonObject;

@Feature (name = "remote")
public interface Remote extends Serializable {
	
	interface Error {
		int UnknownHost = 1100;
		int Timeout 	= 1200;
		int Other 		= 5000;
	}
	
	interface Spec {
		
		String Endpoint 	= "endpoint";
		String Path 		= "path";
		
		String Verb 		= "verb";
		
		String Data 		= "data";
		String Headers 		= "headers";
		
		String TrustAll 	= "trustAll";
		
		String Serializer	= "serializer";
		String Sign			= "sign";
			String SignProtocol		= "protocol";
			String SignKey			= "key";
			String SignSecret		= "secret";
			String SignToken		= "token";
			String SignTokenSecret	= "tokenSecret";
			String User				= "user";
			String Password			= "password";
			String SignReplace		= "replace";
			String SignPayload		= "payload";
			String SignScheme		= "scheme";
			String SignData			= "data";
			String SignAlgorithm	= "algorithm";
			String SignHashing		= "hashing";
			String SignPlaceholder	= "signaturePlaceholder";
			String SignSignatureName= "signatureName";
		
		String Timeout			= "timeout";
			String TimeoutConnect	= "connect";
			String TimeoutWrite		= "write";
			String TimeoutRead		= "read";
		

		String Proxy		= "proxy";
			String ProxyType	= "type";
			String ProxyHost	= "host";
			String ProxyPort	= "port";
		
		String SuccessCode	= "successCode";
		
	}
	
	interface Callback {
		void onError 	(int status, Object message);
		void onSuccess 	(int status, Object payload);
	}

	boolean post 	(JsonObject spec, Callback callback, ApiStreamSource... attachments);
	boolean put 	(JsonObject spec, Callback callback, ApiStreamSource... attachments);
	boolean get 	(JsonObject spec, Callback callback);
	boolean delete 	(JsonObject spec, Callback callback);
	boolean head 	(JsonObject spec, Callback callback);
	boolean patch 	(JsonObject spec, Callback callback);
	
}

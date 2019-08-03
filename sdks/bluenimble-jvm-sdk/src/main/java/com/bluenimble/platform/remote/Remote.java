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

import java.io.IOException;
import java.util.Map;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.json.JsonObject;

@Feature (name = "remote")
public interface Remote extends Recyclable {
	
	interface Error {
		int UnknownHost = 1100;
		int Timeout 	= 1200;
		int Other 		= 5000;
	}
	
	interface Spec {
		
		String Protocol 	= "protocol";
		
		String Host			= "host";
		String Port			= "port";
		
		String Scheme 		= "scheme";
		String Endpoint 	= "endpoint";
		String Path 		= "path";
		
		String Verb 		= "verb";
		
		String Data 		= "data";
		String Headers 		= "headers";
		String Cast 		= "cast";
		String Body 		= "$body";
		
		String Ssl			= "ssl";
			String TrustAll 	= "trustAll";
			String TrustStore 	= "trustStore";
			String KeyStore 	= "keyStore";
				String StoreType 		= "type";
				String StoreAlgorithm 	= "algorithm";
				String StoreStream 		= "stream";
				String StorePassword 	= "password";
				String KeyParaphrase 	= "paraphrase";
		
		String UseStreaming	= "useStreaming";	
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
			String Parameters		= "parameters";
		
		String Timeout			= "timeout";
			String TimeoutConnect	= "connect";
			String TimeoutWrite		= "write";
			String TimeoutRead		= "read";
		

		String Proxy		= "proxy";
			String ProxyType	= "type";
			String ProxyHost	= "host";
			String ProxyPort	= "port";
			
		String  Pool			= "pool";	
		
		String SuccessCode	= "successCode";
		
		String AllowProprietaryAccess = "allowProprietaryAccess";
		
	}
	
	interface Proprietary {
		String Client = "client";
	}

	interface Callback {
		void onStatus 	(int status, boolean chunked, Map<String, Object> headers);
		void onData 	(int status, byte [] chunk) 	throws IOException;
		void onDone 	(int status, Object data) 		throws IOException;
		void onError 	(int status, Object message) 	throws IOException;
	}

	Object 	proprietary (String name);
	
	void post 	(JsonObject spec, Callback callback, ApiStreamSource... attachments);
	void put 	(JsonObject spec, Callback callback, ApiStreamSource... attachments);
	void get 	(JsonObject spec, Callback callback);
	void delete (JsonObject spec, Callback callback);
	void head 	(JsonObject spec, Callback callback);
	void patch 	(JsonObject spec, Callback callback);
	
}

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
package com.bluenimble.platform.api;

import java.util.Collection;
import java.util.Set;

import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.Traceable;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.security.ApiRequestSignerException;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.security.SpaceKeyStore;

public interface ApiSpace extends Traceable, Manageable {
	
	interface Features {
		String Default 		= "default";
		String Provider 	= "provider";
		String Spec 		= "spec";
	}
	
	interface Secrets {
		String Default 		= "default";
	}
	
	interface Selector {
		boolean select (Api api);
	}
	
	interface Endpoint {
		String 		space 		();
		String 		api 		();
		ApiVerb 	verb 		();
		String [] 	resource 	();
	}
	
	interface Spec {
		String Namespace 		= "namespace";
		String Name 			= "name";
		String Description 		= "description";
		
		String Features 		= "features";
		String Keys 			= "keys";
		
		String Tracer			= "tracer";

		interface secrets {
			String Key 			= "key"; 
			String Algorithm 	= "algorithm"; 
			String Age 			= "age"; 
		}
		
		interface Runtime {
			
		}
		
		String Blocked			= "blocked";
	}
	
	KeyPair				getRootKeys 			() 										throws ApiAccessDeniedException; 
	
	String 				getNamespace 			();
	String 				getName 				();
	String 				getDescription 			();

	boolean 			isStarted 				();
	boolean 			isBlocked 				();

	ApiSpace 			create 					(JsonObject oSpace) 					throws ApiManagementException;
	void 				drop 					(String namespace) 						throws ApiManagementException;

	Api 				install 				(String spaceFolder, String apiFile) 	throws ApiManagementException;
	Api 				install 				(ApiStreamSource payload) 				throws ApiManagementException;

	void 				uninstall 				(String api) 							throws ApiManagementException;
	
	void				stop					(String apiNs) 							throws ApiManagementException;
	void				start					(String apiNs) 							throws ApiManagementException;

	void				save					() 										throws ApiManagementException;
	
	void				restart					(String spaceNs) 						throws ApiManagementException;
	void				refresh					(JsonObject descriptor) 				throws ApiManagementException;
	void				alter					(String spaceNs, JsonObject change) 	throws ApiManagementException;

	void				pause					(String apiNs) 							throws ApiManagementException;
	void				resume					(String apiNs) 							throws ApiManagementException;

	void				list					(Selector selector);
	
	ApiSpace 			space 					(String space) 							throws ApiAccessDeniedException;
	Collection<ApiSpace> 
						spaces 					()										throws ApiAccessDeniedException;
	
	void				addFeature				(String name, String feature, String provider, JsonObject spec)											
																						throws ApiManagementException;
	void				deleteFeature			(String name, String feature)			throws ApiManagementException;
	
	void				addSecrets				(String name, JsonObject spec)			throws ApiManagementException;
	JsonObject 			getSecrets				(String name)							throws ApiManagementException;
	void 				deleteSecrets			(String name)							throws ApiManagementException;
	
	JsonObject			getFeatures				();
	
	Object				getRuntime				(String name);

	JsonObject			instance				(DescribeOption... opts) 				throws ApiAccessDeniedException;
	
	Api 				api 					(String api);

	<T>		T			feature 				(Class<T> type, String feature, ApiContext context);
	
	ApiRequest 			request 				(ApiRequest parentRequest, ApiConsumer consumer, Endpoint endpoint);
	
	String				sign					(ApiRequest request, String utcTimestamp, String accessKey, String secretKey, boolean writeToRequest) 	
																						throws ApiRequestSignerException;
	
	CodeExecutor		executor 				();

	void 				addRecyclable 			(String key, Recyclable recyclable);

	Recyclable 			getRecyclable 			(String key);
	
	void 				removeRecyclable 		(String key);
	
	boolean 			containsRecyclable 		(String key);
	
	Set<String> 		getRecyclables 			();
	
	JsonObject			describe 				(DescribeOption... opts);
	
	SpaceKeyStore		keystore				();
	
	JsonArray			keys					();

}

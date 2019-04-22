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

import java.io.Serializable;

import com.bluenimble.platform.json.JsonObject;

public interface ApiServicesManager extends Serializable {

	interface Selector {
		boolean select (ApiService service);
	}
	
	interface GroupingFlow {
		String 		onGroupKey 	(Api api, String groupKey);
		JsonObject 	onService 	(Api api, JsonObject spec, boolean isObject);
	}
	
	GroupingFlow NoGroupingFlow = new GroupingFlow () {
		@Override
		public String onGroupKey(Api api, String groupKey) {
			return groupKey;
		}
		@Override
		public JsonObject onService (Api api, JsonObject spec, boolean isObject) {
			return spec;
		}
	};
	
	void				load 			(Api api) 							throws ApiServicesManagerException;

	void				onStart 		(ApiContext context) 				throws ApiServicesManagerException;
	void				onStop 			(ApiContext context) 				throws ApiServicesManagerException;
	
	void				start 			(ApiVerb verb, String endpoint) 	throws ApiServicesManagerException;
	void				stop 			(ApiVerb verb, String endpoint) 	throws ApiServicesManagerException;
	
	ApiService			put				(ApiResource resource) 				throws ApiServicesManagerException;
	ApiService			get				(ApiVerb verb, String endpoint);
	ApiService			getById			(String id);
	void				delete			(ApiVerb verb, String endpoint) 	throws ApiServicesManagerException;
	
	void				list			(Selector selector);

	boolean 			exists 			(ApiVerb verb, String endpoint);
	
	boolean				isEmpty			(ApiVerb verb);
	
	JsonObject			groupBy			(String property, String groupItemKey, GroupingFlow flow);
	
}

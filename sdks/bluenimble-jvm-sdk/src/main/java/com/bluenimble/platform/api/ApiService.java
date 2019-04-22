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

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable ( name = "ApiService", runtime = "scripting/service")
public interface ApiService extends Manageable {
	
	interface Spec {
		
		String Verb 		= "verb";
		String Endpoint 	= "endpoint";

		String Id 			= "id";
		String Name 		= "name";
		String Description 	= "description";
		
		interface Security		{
			String Enabled		= "enabled";
			String Roles		= "roles";
			String Schemes		= "schemes";
			String Placeholder	= "placeholder";
			String TokenName	= "tokenName";
		}
		
		interface Media	{
			String Resource		= "resource";
			String Engine		= "engine";
			String Processor	= "processor";
			String Extends		= "extends";
			String Cast			= "cast";
		}

		interface Meta	{
			String Tags		= "tags";
			String Links	= "links";
		}
		
		interface Spi		{
			String Function		= "function";
		}

		interface Runtime		{
		}
		
		interface Tracking	{
			String Level		= "level";
			String Feature		= "feature";
		}

		String Status		= "status";
		String Spec 		= "spec";

		String Mock 		= "mock";
		String Output 		= "output";
		String Features 	= "features";
		
	}
	
	ApiVerb 					getVerb 			();
	String 						getEndpoint 		();
	
	String 						getId 				();
	String 						getName 			();
	String 						getDescription 		();
	
	JsonObject 					getSpecification 	();
	
	void 						setHelper			(String key, Object helper);
	Object 						getHelper			(String key);
	
	JsonObject 					getSecurity 		();
	JsonObject 					getRuntime 			();
	JsonObject 					getFeatures 		();
	JsonObject 					getMedia 			();
	JsonObject 					getTracking 		();
	JsonObject 					getSpiDef 			();
	
	ApiStatus					status				();
	void						pause				();
	void						resume				();
	
	ApiServiceSpi				getSpi				();
	
	JsonObject 					getFailure 			();

	JsonObject					toJson 				();

}

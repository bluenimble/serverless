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
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable ( name = "ApiService", runtime = "scripting/service")
public interface ApiService extends Serializable {
	
	interface Spec {
		
		String Verb 		= "verb";
		String Endpoint 	= "endpoint";

		String Name 		= "name";
		String Description 	= "description";
		
		interface Security		{
			String Enabled		= "enabled";
			String Roles		= "roles";
			String Schemes		= "schemes";
			String Placeholder	= "placeholder";
		}
		
		interface Media	{
			String Resource		= "resource";
			String Engine		= "engine";
			String Base			= "base";
		}

		String Status		= "status";
		String Spec 		= "spec";
		String Runtime 		= "runtime";
		String Custom 		= "custom";
		String Mock 		= "mock";
		String Output 		= "output";
		String Features 	= "features";
		
	}
	
	ApiVerb 					getVerb 			();
	String 						getEndpoint 		();
	
	String 						getName 			();
	String 						getDescription 		();
	
	void 						setHelper			(Object helper);
	Object 						getHelper			();
	
	JsonObject 					getSecurity 		();
	JsonObject 					getRuntime 			();
	JsonObject 					getFeatures 		();
	JsonObject 					getMedia 			();
	JsonObject 					getCustom 			();
	
	ApiStatus					status				();
	void						pause				();
	void						resume				();
	
	ApiServiceSpi				getSpi				();
	
	JsonObject 					getFailure 			();

	JsonObject					toJson 				();

}

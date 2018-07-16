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

import com.bluenimble.platform.Traceable;
import com.bluenimble.platform.api.media.ApiMediaProcessor;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidatorException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable ( name = "Api", runtime = "scripting/api")
public interface Api extends Traceable, Manageable {
	
	interface Spec {
		String Namespace 		= "namespace";
		String Name 			= "name";
		String Description 		= "description";
		String Release			= "release";
		String Status			= "status";
		String Features			= "features";
		String Tracer			= "tracer";
		String Markers			= "markers";
		
		interface Media	{
			String Default		= "default";
		}

		interface Security		{
			String Schemes		= "schemes";
			String Auth			= "auth";
			String Encrypt		= "encrypt";
		}
		interface Tracking		{
			String Tracker		= "tracker";
		}
		interface Spi		{
			String Function		= "function";
		}
		interface Runtime		{
			
		}
		
	}
	
	ApiSpace 			space 					();
	
	String 				getNamespace 			();	
	
	String 				getName 				();
	String 				getDescription 			();
	JsonObject 			getRelease 				();
	
	ApiStatus			status					();
	
	JsonObject 			getFailure 				();
	
	JsonObject 			getRuntime 				();

	JsonObject 			getFeatures 			();
	
	JsonObject 			getMedia 				();
	
	JsonObject 			getSecurity 			();
	
	JsonObject 			getTracking 			();
	
	JsonObject 			getSpiDef 				();
	
	ApiSpi 				getSpi 					();
	
	ApiOutput			call					(ApiRequest request) throws ApiServiceExecutionException;
	
	String 				message 				(String lang, String key, Object... args);
	
	JsonObject 			i18n 					(String lang);
	
	ApiServiceValidator getServiceValidator 	();
	ApiResourcesManager getResourcesManager 	();
	ApiServicesManager 	getServicesManager 		();
	
	ApiMediaProcessor	lockupMediaProcessor	(ApiRequest request, ApiService service);
	
	void 				validate 				(ApiConsumer consumer, JsonObject spec, ApiRequest request) 
																						throws ApiServiceValidatorException;
	void 				setHelper				(String key, Object helper);
	Object 				getHelper				(String key);
	
	JsonObject			describe 				(DescribeOption... options);
	
	ClassLoader			getClassLoader 			();
	
}

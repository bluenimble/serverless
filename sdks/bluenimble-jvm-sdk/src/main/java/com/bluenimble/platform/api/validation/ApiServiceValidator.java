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
package com.bluenimble.platform.api.validation;

import java.io.Serializable;

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;

public interface ApiServiceValidator extends Serializable {

	interface Spec {
		
		String Fields		= "fields";
		
		String Name 		= "name";
		String Title 		= "title";
		
		String Type 		= "type";
		
		String Value		= "value";
		
		String Required 	= "required";
		
		String Secret 		= "secret";
		
		String Min 			= "min";
		String Max 			= "max";

		String VType 		= "vType";
		String SType 		= "sType";
		
		String Format 		= "format";
		String TimeZone		= "timeZone";
		
		String Scope		= "scope";
		
		String ErrMsg		= "errMsg";

		String Facets		= "facets";
		
		String ListOfValues	= "lov";
		
		String Strict		= "strict";

	}

	void 			validate (Api api, JsonObject spec, ApiConsumer consumer, ApiRequest request) 
		throws ApiServiceValidatorException;
	
	void 			addTypeValidator (String name, TypeValidator validator);
	
	TypeValidator 	getTypeValidator (String name);
	
	String 			getMessage (Api api, String lang, String key, Object... args);

}

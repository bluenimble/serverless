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

package com.bluenimble.platform.apis.mgm.utils;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonObject;

public class ValueGuesser {
	
	ApiServiceValidator validator;
	
	public void init (Api api) {
		validator = api.getServiceValidator ();
	}
	
	public Object guess (String name, JsonObject spec) {
		Object value = ValidationUtils.guessValue (validator, name, spec);
		if (value == null) {
			return Lang.BLANK;
		}
		if (value instanceof JsonObject) {
			return ((JsonObject)value).toString (0);
		}
		return String.valueOf (value);
	}
	
}
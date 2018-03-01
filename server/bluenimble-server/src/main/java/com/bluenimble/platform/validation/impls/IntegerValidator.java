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
package com.bluenimble.platform.validation.impls;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.json.JsonObject;

public class IntegerValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String Type 				= "Integer";
	
	public static final String TypeMessage			= "IntegerType";
	
	public static final String MinMessage			= "IntegerMin";
	public static final String MaxMessage			= "IntegerMax";
	
	@Override
	public String getName () {
		return Type;
	}

	@Override
	public Object validate (Api api, ApiConsumer consumer, ApiRequest request, 
			DefaultApiServiceValidator validator, String name, String label, JsonObject spec, Object value) {
		
		JsonObject message = isRequired (validator, api, request.getLang (), label, spec, value);
		if (message != null) {
			return message;
		}
		
		if (value == null) {
			return null;
		}
		
		boolean isInteger = false;
		
		int iValue = 0;
		
		if (value instanceof Integer || value.getClass ().equals (Integer.TYPE)) {
			iValue = (Integer)value;
			isInteger = true;
		}
		
		if (!isInteger) {
			try {
				iValue = Integer.parseInt (String.valueOf (value));
				isInteger = true;
			} catch (NumberFormatException nfex) {
			}
		}
		
		if (!isInteger) {
			return ValidationUtils.feedback (
				null, spec, Spec.Type, 
				validator.getMessage (api, request.getLang (), TypeMessage, label)
			);
		}

		// validate length
		
		JsonObject feedback = null;
		
		int min = Json.getInteger (spec, Spec.Min, 1);
		if (iValue < min) {
			feedback = ValidationUtils.feedback (
				feedback, spec, Spec.Min, 
				validator.getMessage (api, request.getLang (), MinMessage, label, String.valueOf (min), String.valueOf (value))
			);
		}
		int max = Json.getInteger (spec, Spec.Max, Integer.MAX_VALUE);
		if (iValue > max) {
			feedback = ValidationUtils.feedback (
				feedback, spec, Spec.Max, 
				validator.getMessage (api, request.getLang (), MaxMessage, label, String.valueOf (max), String.valueOf (value))
			);
		}

		if (feedback == null) {
			return iValue;
		}
		
		return feedback;
	}

}

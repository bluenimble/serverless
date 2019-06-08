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
package com.bluenimble.platform.api.validation.impls.types;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.TypeValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.impls.AbstractTypeValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class FloatValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String TypeMessage			= "FloatType";

	public static final String MinMessage			= "FloatMin";
	public static final String MaxMessage			= "FloatMax";
	
	@Override
	public String getName () {
		return FieldType.Float;
	}

	@Override
	public Object validate (Api api, ApiConsumer consumer, ApiRequest request, 
			ApiServiceValidator validator, String name, String label, JsonObject spec, Object value) {
		
		JsonObject message = isRequired (validator, api, request, label, spec, value);
		if (message != null) {
			return message;
		}
		
		if (value == null) {
			return null;
		}
		
		boolean isFloat = false;
		
		double fValue = 0;
		
		if (value instanceof Float || value.getClass ().equals (Float.TYPE)) {
			fValue = (Float)value;
			isFloat = true;
		}
		
		if (!isFloat) {
			try {
				fValue = Float.parseFloat (String.valueOf (value));
				isFloat = true;
			} catch (NumberFormatException nfex) {
			}
		}
		
		if (!isFloat) {
			return ValidationUtils.feedback (
				null, spec, Spec.Type, 
				validator.getMessage (api, request.getLang (), TypeMessage, label)
			);
		}

		// validate length
		
		JsonObject feedback = null;
		
		String min = ValidationUtils.isValidRestriction (spec, fValue, Spec.Min);
		if (min != null) {
			feedback = ValidationUtils.feedback (
				feedback, spec, Spec.Min, 
				validator.getMessage (api, request.getLang (), MinMessage, label, min, String.valueOf (value))
			);
		}
		String max = ValidationUtils.isValidRestriction (spec, fValue, Spec.Max);
		if (max != null) {
			feedback = ValidationUtils.feedback (
				feedback, spec, Spec.Max, 
				validator.getMessage (api, request.getLang (), MaxMessage, label, max, String.valueOf (value))
			);
		}

		if (feedback != null) {
			return feedback;
		}
		
		JsonObject enumFeedback = ValidationUtils.checkEnum (api, request, validator, spec, label, value, feedback);
		if (feedback == null) {
			feedback = enumFeedback;
		}
		
		if (feedback == null) {
			return fValue;
		}
		
		return feedback;
	}


	@Override
	public Object guessValue (ApiServiceValidator validator, String name, JsonObject spec) {
		
		Object value = null;
		
		Object _enum = spec.get (Spec.Enum);
		if (_enum != null) {
			if (_enum instanceof JsonArray) {
				value = ((JsonArray)_enum).get (0);
			} else if (_enum instanceof JsonObject) {
				value = ((JsonObject)_enum).keySet ().toArray () [0];
			}
		}
		if (value != null) {
			return value;
		}
		
		// vType
		String vType = Json.getString (spec, Spec.VType);
		
		if (!Lang.isNullOrEmpty (vType)) {
			TypeValidator vTypeValiator = validator.getTypeValidator (vType);
			if (vTypeValiator != null) {
				value = vTypeValiator.guessValue (validator, name, spec);
			}
		}
		if (value != null) {
			return value;
		}
		
		double min = Json.getDouble (spec, Spec.Min, 1);
		
		return ( min + Json.getDouble (spec, Spec.Max, min) ) / 2;
	}
}

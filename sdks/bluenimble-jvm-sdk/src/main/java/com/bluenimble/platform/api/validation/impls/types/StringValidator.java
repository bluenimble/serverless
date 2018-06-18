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
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.TypeValidator;
import com.bluenimble.platform.api.validation.impls.AbstractTypeValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class StringValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String SecretValue			= "*******";
	
	public static final String Type 				= "String";
	
	public static final String MinMessage			= "StringMin";
	public static final String MaxMessage			= "StringMax";
	public static final String LovMessage			= "StringLov";
	
	public static final String FormatMessage		= "StringFormat";
	
	@Override
	public String getName () {
		return Type;
	}

	@Override
	public Object validate (Api api, ApiConsumer consumer, ApiRequest request, 
			ApiServiceValidator validator, String name, String label, JsonObject spec, Object value) {
		
		Object message = isRequired (validator, api, request.getLang (), label, spec, value);
		if (message != null) {
			return message;
		}
		
		if (value == null) {
			return null;
		}
		
		JsonObject feedback = null;
		
		String sValue = String.valueOf (value);
		
		String displayValue = Json.getBoolean (spec, Spec.Secret, false) ? SecretValue : sValue;
		
		// validate length
		int min = Json.getInteger (spec, Spec.Min, 0);
		if (sValue.length () < min) {
			feedback = ValidationUtils.feedback (
				feedback, spec, Spec.Min, 
				validator.getMessage (api, request.getLang (), MinMessage, label, String.valueOf (min), displayValue)
			);
		}
		int max = Json.getInteger (spec, Spec.Max, Integer.MAX_VALUE);
		if (sValue.length () > max) {
			feedback = ValidationUtils.feedback (
				feedback, spec, Spec.Max, 
				validator.getMessage (api, request.getLang (), MaxMessage, label, String.valueOf (max), displayValue)
			);
		}
		
		if (feedback != null) {
			return feedback;
		}
		
		// validate vType
		String vType = Json.getString (spec, Spec.VType);
		
		if (!Lang.isNullOrEmpty (vType)) {
			TypeValidator vTypeValiator = validator.getTypeValidator (vType);
			if (vTypeValiator != null) {
				message = vTypeValiator.validate (api, consumer, request, validator, name, label, spec, sValue);
				if (message != null) {
					return message;
				}
			}
		}
		
		JsonObject lovFeedback = ValidationUtils.checkListOfValues (api, request, validator, spec, label, sValue, feedback);
		if (lovFeedback != null) {
			return lovFeedback;
		}
		
		return null;
	}

	@Override
	public Object guessValue (ApiServiceValidator validator, String name, JsonObject spec) {
		
		Object value = null;
		
		Object lov = spec.get (Spec.ListOfValues);
		if (lov != null) {
			if (lov instanceof JsonArray) {
				value = ((JsonArray)lov).get (0);
			} else if (lov instanceof JsonObject) {
				value = ((JsonObject)lov).keySet ().toArray () [0];
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
		
		int length = ( Json.getInteger (spec, Spec.Min, 2) + Json.getInteger (spec, Spec.Max, 10) ) / 2;
		
		return Lang.UUID (length);
	}

}

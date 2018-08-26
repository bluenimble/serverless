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
package com.bluenimble.platform.api.validation.impls;

import java.text.MessageFormat;
import java.util.ArrayList;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.TypeValidator;
import com.bluenimble.platform.api.validation.impls.types.StringValidator;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class ValidationUtils {

	public static JsonObject feedback (JsonObject feedback, JsonObject spec, String facet, String message) {
		if (Lang.isNullOrEmpty (facet)) {
			facet = AbstractTypeValidator.RequiredFacet;
		}
		if (feedback == null) {
			feedback = new JsonObject ();
			if (!AbstractTypeValidator.RequiredFacet.equals (facet)) {
				feedback.set (ApiService.Spec.Spec, spec);
			}
			feedback.set (Spec.Facets, new JsonObject ());
		}
		
		Json.getObject (feedback, ApiServiceValidator.Spec.Facets).set (facet, message);
		
		return feedback;
	}
	
	public static Object guessValue (ApiServiceValidator validator, String name, JsonObject spec) {
		TypeValidator tv = validator.getTypeValidator (Json.getString (spec, ApiServiceValidator.Spec.Type, FieldType.String));
		if (tv == null) {
			return null;
		}
		return tv.guessValue (validator, name, spec);
	}
	
	@SuppressWarnings("unchecked")
	public static JsonObject checkEnum (Api api, ApiRequest request, 
			ApiServiceValidator validator, JsonObject spec, String label, String value, JsonObject feedback) {
		
		Object _enum = spec.get (Spec.Enum);
		if (_enum == null) {
			return null;
		}
		
		boolean enumFailed = false;
		
		String sEnum = null;
		
		if (_enum instanceof JsonArray) {
			enumFailed = !((JsonArray)_enum).contains (value);
			sEnum = ((JsonArray)_enum).join (Lang.COMMA, false);
		} else if (_enum instanceof JsonObject) {
			enumFailed = !((JsonObject)_enum).containsKey (value);
			sEnum = Lang.join (new ArrayList<String> (((JsonObject)_enum).keySet ()), Lang.COMMA);
		}
		
		if (enumFailed) {
			// custom message
			String msg = null; 
			if (spec.containsKey (Spec.ErrMsg)) {
				msg = MessageFormat.format (spec.getString (Spec.ErrMsg), new Object [] { label, sEnum, value });
			} else {
				msg = validator.getMessage (api, request.getLang (), StringValidator.EnumMessage, label, sEnum, value);
			}
			// custom status code
			int status = Json.getInteger (spec, Spec.ErrCode, 0);
			if (status > 0) {
				request.set (ApiRequest.ResponseStatus, new ApiResponse.Status (status));
			}
			return ValidationUtils.feedback (feedback, spec, Spec.Enum, msg);
		}
		return null;
	}
	
	public static String isValidRestriction (JsonObject spec, double dValue, String restriction) {
		
		if (!spec.containsKey (restriction)) {
			return null;
		}
		
		boolean exclusive = false;
		String restrictValue = Json.getString (spec, restriction).trim ();
		if (Lang.isNullOrEmpty (restrictValue)) {
			return null;
		}
		if (restrictValue.startsWith (ApiServiceValidator.Spec.Exclusive)) {
			exclusive = true;
			restrictValue = restrictValue.substring (1);
		}

		// resolve value
		double dRestriction;
		try {
			dRestriction = Double.parseDouble (restrictValue);
		} catch (NumberFormatException nfex) {
			return restrictValue;
		}
		
		if (exclusive) {
			if (Spec.Min.equals (restriction)) {
				return dValue > dRestriction ? null : restrictValue;
			} else if (Spec.Max.equals (restriction)) {
				return dValue < dRestriction ? null : restrictValue;
			} 
		} else {
			if (Spec.Min.equals (restriction)) {
				return dValue >= dRestriction ? null : restrictValue;
			} else if (Spec.Max.equals (restriction)) {
				return dValue <= dRestriction ? null : restrictValue;
			} 
		}
		
		return null;
	}
	
}

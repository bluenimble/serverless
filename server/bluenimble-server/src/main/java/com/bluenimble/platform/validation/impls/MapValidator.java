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

import java.io.InputStream;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.ApiServiceValidatorException;
import com.bluenimble.platform.json.JsonException;
import com.bluenimble.platform.json.JsonObject;

public class MapValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String Type 				= "Map";
	public static final String AltType 				= "Object";
	
	@Override
	public String getName () {
		return Type;
	}

	@SuppressWarnings("unchecked")
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
		
		boolean updateRequest = false;
		
		Map<String, Object> object = null;
		if (value instanceof JsonObject) {
			object = (Map<String, Object>)value;
		} else if (value instanceof InputStream) {
			try {
				object = new JsonObject (Json.load ((InputStream)value));
			} catch (Exception e) {
				return ValidationUtils.feedback (
					null, spec, Spec.Type, 
					e.getMessage ()
				);
			}
			updateRequest = true;
		} else {
			try {
				object = new JsonObject (String.valueOf (value));
			} catch (JsonException e) {
				return ValidationUtils.feedback (
					null, spec, Spec.Type, 
					e.getMessage ()
				);
			}
			updateRequest = true;
		}
		
		if (object.isEmpty ()) {
			return ValidationUtils.feedback (
				null, spec, null, 
				validator.getMessage (api, request.getLang (), RequiredMessage, label)
			);
		}
		
		try {
			validator.validate (api, spec, consumer, request, object);
		} catch (ApiServiceValidatorException e) {
			return e.getFeedback ();
		}
		
		if (updateRequest) {
			request.set (name, object, ApiRequest.Scope.Parameter);
		}
		
		return null;
	}

}

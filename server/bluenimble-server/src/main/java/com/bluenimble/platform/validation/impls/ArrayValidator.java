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
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonException;
import com.bluenimble.platform.json.JsonObject;

public class ArrayValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String Type 				= "Array";
	
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
		
		boolean updateRequest = false;
		
		JsonArray array = null;
		if (value instanceof JsonArray) {
			array = (JsonArray)value;
		} else {
			try {
				array = new JsonArray (String.valueOf (value));
			} catch (JsonException e) {
				return ValidationUtils.feedback (
					null, spec, Spec.Type, 
					e.getMessage ()
				);
			}
			updateRequest = true;
		}
		
		if (array.isEmpty ()) {
			return ValidationUtils.feedback (
				null, spec, null, 
				validator.getMessage (api, request.getLang (), RequiredMessage, label)
			);
		}
		
		String sType = Json.getString (spec, Spec.SType, MapValidator.Type);
		
		TypeValidator tValidator = validator.getValidator (sType);
		
		for (int i = 0; i < array.count (); i++) {
			Object feedback = tValidator.validate (api, consumer, request, validator, name, label + "->index " + i, spec, array.get (i));
			if (feedback != null) {
				return feedback;
			}
		}
		
		if (updateRequest) {
			request.set (name, array);
		}

		return null;
	}

}

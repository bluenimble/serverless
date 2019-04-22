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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.ApiServiceValidatorException;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.impls.AbstractTypeValidator;
import com.bluenimble.platform.api.validation.impls.DefaultApiServiceValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonException;
import com.bluenimble.platform.json.JsonObject;

public class MapValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String StrictMessage		= "ObjectStrict";
	
	public static final String GuessKey				= "key";
	public static final String GuessValue			= "value";
	
	public static final String MinMessage			= "ObjectMin";
	public static final String MaxMessage			= "ObjectMax";
	
	
	@Override
	public String getName () {
		return FieldType.Object;
	}

	@SuppressWarnings("unchecked")
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
		
		boolean updateRequest = false;
		
		JsonObject object = null;
		if (value instanceof JsonObject) {
			object = (JsonObject)value;
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
			String sValue = String.valueOf (value);
			if (!sValue.startsWith (Lang.OBJECT_OPEN)) {
				sValue = Lang.OBJECT_OPEN + sValue + Lang.OBJECT_CLOSE;
			}
			try {
				object = new JsonObject (sValue);
			} catch (JsonException e) {
				return ValidationUtils.feedback (
					null, spec, Spec.Type, 
					e.getMessage ()
				);
			}
			updateRequest = true;
		}
		
		// shrink
		if (Json.getBoolean (spec, Spec.Shrink, Json.getBoolean (api.getRuntime (), Spec.Shrink, false))) {
			object.shrink ();
		}
		
		if (object.isEmpty () && Json.getBoolean (spec, Spec.Required, true)) {
			return ValidationUtils.feedback (
				null, spec, null, 
				validator.getMessage (api, request.getLang (), RequiredMessage, label)
			);
		}
		
		JsonObject feedback = null;
		String min = ValidationUtils.isValidRestriction (spec, object.count(), Spec.MinSize);
		if (min != null) {
			feedback = ValidationUtils.feedback (
				feedback, spec, Spec.MinSize, 
				validator.getMessage (api, request.getLang (), MinMessage, label, min, String.valueOf (value))
			);
		}
		String max = ValidationUtils.isValidRestriction (spec, object.count(), Spec.MaxSize);
		if (max != null) {
			feedback = ValidationUtils.feedback (
				feedback, spec, Spec.MaxSize, 
				validator.getMessage (api, request.getLang (), MaxMessage, label, max, String.valueOf (value))
			);
		}

		if (feedback != null) {
			return feedback;
		}
		
		// check strict
		boolean strict = Json.getBoolean (spec, Spec.Strict, Json.getBoolean (api.getRuntime (), Spec.Strict, false));
		if (strict && !object.isEmpty ()) {
			List<String> failingFields = new ArrayList<String> ();
			Set<String> fields = object.keySet ();
			for (String field : fields) {
				if (!object.containsKey (field)) {
					failingFields.add (field);
				}
			}
			if (!failingFields.isEmpty ()) {
				return ValidationUtils.feedback (
					null, spec, Spec.Strict, 
					validator.getMessage (api, request.getLang (), StrictMessage, label, Lang.join (failingFields, Lang.COMMA))
				);
			}
		}
		
		try {
			((DefaultApiServiceValidator)validator).validate (api, spec, consumer, request, object);
		} catch (ApiServiceValidatorException e) {
			return e.getFeedback ();
		}
		
		if (updateRequest) {
			request.set (name, object, ApiRequest.Scope.Parameter);
		}
		
		return null;
	}

	@Override
	public Object guessValue (ApiServiceValidator validator, String name, JsonObject spec) {
		
		JsonObject fields = Json.getObject (spec, Spec.Fields);
		if (Json.isNullOrEmpty (fields)) {
			return new JsonObject ().set (GuessKey, GuessValue);
		}
		
		JsonObject buffer = new JsonObject ();
		
		Iterator<String> keys = fields.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			buffer.set (key, ValidationUtils.guessValue (validator, key, Json.getObject (fields, key)));
		}
		
		return buffer;
		
	}
		
}

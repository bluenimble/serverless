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

import java.io.UnsupportedEncodingException;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.TypeValidator;
import com.bluenimble.platform.api.validation.impls.AbstractTypeValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonObject;

public class Base64Validator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String TypeMessage			= "Base64Type";
	
	@Override
	public String getName () {
		return FieldType.Base64;
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
		
		String sValue = String.valueOf (value);
		
		byte [] content = null;
		
		try {
			content = Base64.decodeBase64 (sValue);
		} catch (Exception ex) {
			// Ignore
		}
		
		if (content == null) {
			return ValidationUtils.feedback (
				null, spec, Spec.Type, 
				validator.getMessage (api, request.getLang (), TypeMessage, label)
			);
		}
		
		try {
			sValue = new String (content, Json.getString (spec, Spec.Charset, Encodings.UTF8));
		} catch (UnsupportedEncodingException e) {
			return ValidationUtils.feedback (
				null, spec, Spec.Type,
				e.getMessage ()
			);
		}
		
		String sType = Json.getString (spec, Spec.SType, FieldType.String);
		
		TypeValidator tValidator = validator.getTypeValidator (sType);
		if (tValidator == null) {
			tValidator = validator.getTypeValidator (FieldType.Object);
		}
		
		Object feedback = tValidator.validate (api, consumer, request, validator, name, label, spec, sValue);
		if (feedback != null) {
			return feedback;
		}
		
		if (!MapValidator.class.equals (tValidator.getClass ()) && !ArrayValidator.class.equals (tValidator.getClass ())) {
			return sValue;
		}
		
		return null;

	}

}

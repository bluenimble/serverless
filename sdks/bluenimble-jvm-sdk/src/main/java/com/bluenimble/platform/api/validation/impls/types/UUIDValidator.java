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

import java.text.MessageFormat;
import java.util.UUID;

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.impls.AbstractTypeValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonObject;

public class UUIDValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String TypeMessage 	= "UUIDType";
	
	@Override
	public String getName () {
		return FieldType.UUID;
	}

	@Override
	public Object validate (Api api, ApiConsumer consumer, ApiRequest request, 
			ApiServiceValidator validator, String name, String label, JsonObject spec, Object value) {
		
		Object message = isRequired (validator, api, request, label, spec, value);
		if (message != null) {
			return message;
		}
		
		if (value == null) {
			return null;
		}
		
		try {
			UUID.fromString (String.valueOf (value));
		} catch (IllegalArgumentException ex) {
			String msg = null; 
			if (spec.containsKey (Spec.ErrMsg)) {
				msg = MessageFormat.format (spec.getString (Spec.ErrMsg), new Object [] { label });
			} else {
				msg = validator.getMessage (api, request.getLang (), TypeMessage, label );
			}
			return ValidationUtils.feedback (
				null, spec, Spec.Type, 
				msg
			);
		}
		
		return null;
	}
	
	@Override
	public Object guessValue (ApiServiceValidator validator, String name, JsonObject spec) {
		return UUID.randomUUID ().toString ();
	}

}

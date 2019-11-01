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

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.impls.AbstractTypeValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonObject;

public class RegexValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String TypeMessage			= "Regex";
	
	@Override
	public String getName () {
		return FieldType.Facets.Regex;
	}

	@Override
	public Object validate (Api api, ApiConsumer consumer, ApiRequest request, 
			ApiServiceValidator validator, String name, String label, JsonObject spec, Object value) {
		
		if (value == null) {
			return null;
		}
		
		String regex = Json.getString (spec, Spec.Format);
		if (regex == null) {
			return null;
		}
		
		if (!String.valueOf (value).matches (regex)) {
			// custom message
			String msg = null; 
			if (spec.containsKey (Spec.ErrMsg)) {
				msg = MessageFormat.format (spec.getString (Spec.ErrMsg), new Object [] { label });
			} else {
				msg = validator.getMessage (api, request.getLang (), TypeMessage, label);
			}
			// custom status code
			int status = Json.getInteger (spec, Spec.ErrCode, 0);
			if (status > 0) {
				request.set (ApiRequest.ResponseStatus, new ApiResponse.Status (status));
			}
			return ValidationUtils.feedback (
				null, spec, Spec.Format, 
				msg
			);
		}
		
		return null;
	}
	
	public static void main (String [] args) {
		String regex = "^[a-zA-Z0-9\\.\\_\\-#@]+$";
		System.out.println ("alp#ha1_.@".matches (regex));
	}

}

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
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.json.JsonObject;

public abstract class AbstractTypeValidator implements TypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String RequiredFacet	= "required";

	public static final String RequiredMessage	= "Required";
	
	protected JsonObject isRequired (DefaultApiServiceValidator validator, Api api, String lang, String label, JsonObject spec, Object value) {
		boolean required = Json.getBoolean (spec, Spec.Required, true);
		if (required) {
			if (value == null) {
				return ValidationUtils.feedback (null, spec, RequiredFacet, validator.getMessage (api, lang, RequiredMessage, label));
			} else 
			if (value instanceof String && Lang.isNullOrEmpty ((String)value)) {
				return ValidationUtils.feedback (null, spec, RequiredFacet, validator.getMessage (api, lang, RequiredMessage, label));
			}
		}
		return null;
	}
	
}

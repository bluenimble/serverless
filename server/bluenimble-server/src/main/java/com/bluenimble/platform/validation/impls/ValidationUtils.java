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
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
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
	
}

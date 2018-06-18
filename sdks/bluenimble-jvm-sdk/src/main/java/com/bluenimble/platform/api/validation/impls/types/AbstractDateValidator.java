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

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.impls.AbstractTypeValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonObject;

public abstract class AbstractDateValidator extends AbstractTypeValidator {

	private static final long serialVersionUID = 2430274897113013353L;
	
	public static final String TypeMessage			= "DateType";
	public static final String MinMessage			= "DateMin";
	public static final String MaxMessage			= "DateMax";
	
	protected abstract String getDefaultFormat ();
	
	@Override
	public Object validate (Api api, ApiConsumer consumer, ApiRequest request, 
				ApiServiceValidator validator, String name, String label, JsonObject spec, Object value) {
		
		JsonObject message = isRequired (validator, api, request.getLang (), label, spec, value);
		if (message != null) {
			return message;
		}
		
		if (value == null) {
			return null;
		}
		
		Date date = null;
		
		if (Date.class.isAssignableFrom (value.getClass ())) {
			date = (Date)value;
		}
		
		if (date != null) {
			return null;
		}
		
		String format = Json.getString (spec, Spec.Format);
		if (Lang.isNullOrEmpty (format)) {
			format = getDefaultFormat ();
		}
		
		String tz = Json.getString (spec, Spec.TimeZone);
		
		try {
			date = Lang.toDate (String.valueOf (value), format, tz == null ? Lang.UTC_TZ : TimeZone.getTimeZone (tz));
		} catch (ParseException nfex) {
		}
		
		if (date == null) {
			return ValidationUtils.feedback (
				null, spec, Spec.Type, 
				validator.getMessage (api, request.getLang (), TypeMessage, label, format)
			);
		}
		
		JsonObject feedback = null;

		String sMin = Json.getString (spec, Spec.Min);
		if (!Lang.isNullOrEmpty (sMin)) {
			Date min = null;
			try {
				min = Lang.toDate (sMin, format);
			} catch (ParseException nfex) {
			}
			if (min != null && date.getTime () < min.getTime ()) {
				feedback = ValidationUtils.feedback (
					feedback, spec, Spec.Min, 
					validator.getMessage (api, request.getLang (), MinMessage, label, sMin)
				);
			}
		}
		
		String sMax = Json.getString (spec, Spec.Max);
		if (!Lang.isNullOrEmpty (sMax)) {
			Date max = null;
			try {
				max = Lang.toDate (sMax, format);
			} catch (ParseException nfex) {
			}
			if (max != null && date.getTime () > max.getTime ()) {
				feedback = ValidationUtils.feedback (
					feedback, spec, Spec.Max, 
					validator.getMessage (api, request.getLang (), MaxMessage, label, sMax)
				);
			}
		}
		
		if (feedback == null) {
			return date;
		}
		
		return feedback;
	}

}

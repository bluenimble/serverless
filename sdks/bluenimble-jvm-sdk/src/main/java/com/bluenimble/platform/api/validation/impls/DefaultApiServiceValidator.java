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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidatorException;
import com.bluenimble.platform.api.validation.TypeValidator;
import com.bluenimble.platform.api.validation.impls.types.AlphaNumericValidator;
import com.bluenimble.platform.api.validation.impls.types.ArrayValidator;
import com.bluenimble.platform.api.validation.impls.types.BooleanValidator;
import com.bluenimble.platform.api.validation.impls.types.ContainsValidator;
import com.bluenimble.platform.api.validation.impls.types.DateTimeValidator;
import com.bluenimble.platform.api.validation.impls.types.DateValidator;
import com.bluenimble.platform.api.validation.impls.types.DecimalValidator;
import com.bluenimble.platform.api.validation.impls.types.EmailValidator;
import com.bluenimble.platform.api.validation.impls.types.EndsWithValidator;
import com.bluenimble.platform.api.validation.impls.types.IntegerValidator;
import com.bluenimble.platform.api.validation.impls.types.LongValidator;
import com.bluenimble.platform.api.validation.impls.types.MapValidator;
import com.bluenimble.platform.api.validation.impls.types.PhoneValidator;
import com.bluenimble.platform.api.validation.impls.types.RegexValidator;
import com.bluenimble.platform.api.validation.impls.types.StartsWithValidator;
import com.bluenimble.platform.api.validation.impls.types.StreamValidator;
import com.bluenimble.platform.api.validation.impls.types.StringValidator;
import com.bluenimble.platform.api.validation.impls.types.UrlValidator;
import com.bluenimble.platform.json.JsonObject;

public class DefaultApiServiceValidator implements ApiServiceValidator {

	private static final long serialVersionUID = 7521000052639259961L;
		
	private static final char ConsumerScope = 'c';

	private static final Map<String, Scope> Scopes 	= new HashMap<String, Scope> ();
	static {
		// scopes
		Scopes.put ("h", Scope.Header);
		Scopes.put ("p", Scope.Parameter);
		Scopes.put ("s", Scope.Stream);
	}
	
	private static final String DefaultScope = "p";
	
	private static final String RawType		 = "Raw";
	
	private Map<String, TypeValidator> validators = new HashMap<String, TypeValidator> ();
	
	public DefaultApiServiceValidator () {
		// validators
		addTypeValidator (StringValidator.Type.toLowerCase (), 			new StringValidator ());
		addTypeValidator (BooleanValidator.Type.toLowerCase (), 		new BooleanValidator ());
		addTypeValidator (IntegerValidator.Type.toLowerCase (), 		new IntegerValidator ());
		addTypeValidator (LongValidator.Type.toLowerCase (), 			new LongValidator ());
		addTypeValidator (DecimalValidator.Type.toLowerCase (), 		new DecimalValidator ());
		addTypeValidator (DateValidator.Type.toLowerCase (), 			new DateValidator ());
		addTypeValidator (DateTimeValidator.Type.toLowerCase (), 		new DateTimeValidator ());
		addTypeValidator (MapValidator.Type.toLowerCase (), 			new MapValidator ());
		addTypeValidator (MapValidator.AltType.toLowerCase (), 			new MapValidator ());
		addTypeValidator (ArrayValidator.Type.toLowerCase (), 			new ArrayValidator ());
		addTypeValidator (StreamValidator.Type.toLowerCase (), 			new StreamValidator ());
		addTypeValidator (AlphaNumericValidator.Type.toLowerCase (), 	new AlphaNumericValidator ());
		
		// vtypes
		addTypeValidator (StartsWithValidator.Type.toLowerCase (), 		new StartsWithValidator ());
		addTypeValidator (EndsWithValidator.Type.toLowerCase (), 		new EndsWithValidator ());
		addTypeValidator (ContainsValidator.Type.toLowerCase (), 		new ContainsValidator ());

		addTypeValidator (EmailValidator.Type.toLowerCase (), 			new EmailValidator ());
		addTypeValidator (UrlValidator.Type.toLowerCase (), 			new UrlValidator ());
		addTypeValidator (PhoneValidator.Type.toLowerCase (), 			new PhoneValidator ());
		
		addTypeValidator (RegexValidator.Type.toLowerCase (), 			new RegexValidator ());
	}
	
	@Override
	public void validate (Api context, JsonObject spec, ApiConsumer consumer, ApiRequest request) 
		throws ApiServiceValidatorException {
		validate (context, spec, consumer, request, null);
	}
	
	public void validate (Api context, JsonObject spec, ApiConsumer consumer, ApiRequest request, Map<String, Object> data) 
		throws ApiServiceValidatorException {
		
		if (spec == null || spec.isEmpty ()) {
			return;
		}
		
		JsonObject oFields = Json.getObject (spec, Spec.Fields);
		
		if (oFields == null || oFields.isEmpty ()) {
			return;
		}
		
		JsonObject feedback = null;
		
		Iterator<String> fields = oFields.keys ();
		while (fields.hasNext ()) {
			String name = fields.next ();
			
			Object oSpec = oFields.get (name);
			if (! (oSpec instanceof JsonObject) ) {
				if (feedback == null) {
					feedback = new JsonObject ();
				}
				feedback.set (name, ValidationUtils.feedback (
					null, spec, Spec.Type, 
					"field definition should be a valid object"
				));
				continue;
			}
			
			JsonObject fSpec = (JsonObject)oSpec;
			
			String type = fSpec.getString (Spec.Type);
			if (Lang.isNullOrEmpty (type)) {
				type = StringValidator.Type;
			}
			
			if (type.equalsIgnoreCase (RawType)) {
				continue;
			}
			
			TypeValidator validator = getTypeValidator (type);
			if (validator == null) {
				if (feedback == null) {
					feedback = new JsonObject ();
				}
				feedback.set (name, ValidationUtils.feedback (
					null, spec, Spec.Type, 
					"type '" + type + "' not supported"
				));
				continue;
			}
			
			String label = getLabel (name, fSpec.getString (Spec.Title));
			
			Object value = valueOf (name, fSpec, request, consumer, data);
			
			Object message = validator.validate (
				context,
				consumer, 
				request, 	
				this,
				name,
				label,
				fSpec, 
				value
			);
						
			if (message != null) {
				if (message instanceof JsonObject) {
					// it's an error
					if (feedback == null) {
						feedback = new JsonObject ();
					}
					feedback.set (name, message);
				} else {
					// it a value cast. set new value...
					if (data != null) {
						data.put (name, message);
					} else {
						request.set (name, message, Scope.Parameter);
					}
				}
			}
		}
		
		if (feedback != null && !feedback.isEmpty ()) {
			throw new ApiServiceValidatorException (feedback);
		}
		
	}

	private String getLabel (String name, String title) {
		if (Lang.isNullOrEmpty (title)) {
			return name;
		}
		return title;
	}
	
	@Override
	public void addTypeValidator (String name, TypeValidator validator) {
		validators.put (name.toLowerCase (), validator);
	}

	@Override
	public TypeValidator getTypeValidator (String name) {
		return validators.get (name.toLowerCase ());
	}

	@Override
	public String getMessage (Api api, String lang, String key, Object... args) {
		return api.message (lang, key, args);
	}
	
	public Object valueOf (String name, JsonObject fieldSpec, ApiRequest request, ApiConsumer consumer, Map<String, Object> data) {
		
		if (data != null) {
			return data.get (name);
		}
		
		Object value = null;
		
		String s = fieldSpec.getString (Spec.Scope);
		if (Lang.isNullOrEmpty (s)) {
			s = DefaultScope;
		}
		
		Object defaultValue = fieldSpec.get (Spec.Value);
		
		Scope scope = null;
		s = s.trim ();
		for (int i = 0; i < s.length (); i++) {
			char sc = s.charAt (i);
			if (ConsumerScope == sc) {
				if (defaultValue != null) {
					value = consumer.get (defaultValue.toString ());
				}
				if (value != null) {
					request.set (name, value);
				}
				continue;
			}
			scope = Scopes.get (String.valueOf (sc));
			if (scope == null) {
				continue;
			}
			value = request.get (name, scope);
			if (value != null) {
				break;
			}
		}
		
		if (value == null) {
			value = defaultValue;
			if (value != null) {
				request.set (name, value);
			}
		}
		
		return value;
		
	}

}

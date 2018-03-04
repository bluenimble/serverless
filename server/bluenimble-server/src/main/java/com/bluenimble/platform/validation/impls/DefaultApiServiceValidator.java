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
	
	private Map<String, TypeValidator> vtypes = new HashMap<String, TypeValidator> ();
	
	public DefaultApiServiceValidator () {
		// validators
		validators.put (StringValidator.Type.toLowerCase (), 		new StringValidator ());
		validators.put (BooleanValidator.Type.toLowerCase (), 		new BooleanValidator ());
		validators.put (IntegerValidator.Type.toLowerCase (), 		new IntegerValidator ());
		validators.put (LongValidator.Type.toLowerCase (), 			new LongValidator ());
		validators.put (DecimalValidator.Type.toLowerCase (), 		new DecimalValidator ());
		validators.put (DateValidator.Type.toLowerCase (), 			new DateValidator ());
		validators.put (DateTimeValidator.Type.toLowerCase (), 		new DateTimeValidator ());
		validators.put (MapValidator.Type.toLowerCase (), 			new MapValidator ());
		validators.put (MapValidator.AltType.toLowerCase (), 		new MapValidator ());
		validators.put (ArrayValidator.Type.toLowerCase (), 		new ArrayValidator ());
		validators.put (StreamValidator.Type.toLowerCase (), 		new StreamValidator ());
		validators.put (AlphaNumericValidator.Type.toLowerCase (), 	new AlphaNumericValidator ());
		
		vtypes.put (StartsWithValidator.Type.toLowerCase (), 		new StartsWithValidator ());
		vtypes.put (EndsWithValidator.Type.toLowerCase (), 			new EndsWithValidator ());
		vtypes.put (ContainsValidator.Type.toLowerCase (), 			new ContainsValidator ());

		vtypes.put (EmailValidator.Type.toLowerCase (), 			new EmailValidator ());
		vtypes.put (UrlValidator.Type.toLowerCase (), 				new UrlValidator ());
		vtypes.put (RegexValidator.Type.toLowerCase (), 			new RegexValidator ());
		
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
			
			TypeValidator validator = getValidator (type);
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
	
	public String getMessage (Api api, String lang, String key, Object... args) {
		return api.message (lang, key, args);
	}
	
	public TypeValidator getValidator (String name) {
		return validators.get (name.toLowerCase ());
	}

	public TypeValidator getVType (String name) {
		return vtypes.get (name.toLowerCase ());
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

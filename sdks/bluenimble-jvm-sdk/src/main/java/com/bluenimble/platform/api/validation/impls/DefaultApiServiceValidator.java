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
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.TypeValidator;
import com.bluenimble.platform.api.validation.impls.types.AlphaNumericValidator;
import com.bluenimble.platform.api.validation.impls.types.ArrayValidator;
import com.bluenimble.platform.api.validation.impls.types.Base64Validator;
import com.bluenimble.platform.api.validation.impls.types.BinaryValidator;
import com.bluenimble.platform.api.validation.impls.types.BooleanValidator;
import com.bluenimble.platform.api.validation.impls.types.ContainsValidator;
import com.bluenimble.platform.api.validation.impls.types.DateTimeValidator;
import com.bluenimble.platform.api.validation.impls.types.DateValidator;
import com.bluenimble.platform.api.validation.impls.types.DecimalValidator;
import com.bluenimble.platform.api.validation.impls.types.EmailValidator;
import com.bluenimble.platform.api.validation.impls.types.EndsWithValidator;
import com.bluenimble.platform.api.validation.impls.types.FloatValidator;
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
	
	private Map<String, TypeValidator> validators = new HashMap<String, TypeValidator> ();
	
	public DefaultApiServiceValidator () {
		// validators
		addTypeValidator (FieldType.String.toLowerCase (), 			new StringValidator ());
		addTypeValidator (FieldType.AlphaNumeric.toLowerCase (), 	new AlphaNumericValidator ());
		
		addTypeValidator (FieldType.Boolean.toLowerCase (), 		new BooleanValidator ());
		
		addTypeValidator (FieldType.Integer.toLowerCase (), 		new IntegerValidator ());
		addTypeValidator (FieldType.Long.toLowerCase (), 			new LongValidator ());
		addTypeValidator (FieldType.Decimal.toLowerCase (), 		new DecimalValidator ());
		addTypeValidator (FieldType.Float.toLowerCase (), 			new FloatValidator ());
		
		addTypeValidator (FieldType.Date.toLowerCase (), 			new DateValidator ());
		addTypeValidator (FieldType.DateTime.toLowerCase (), 		new DateTimeValidator ());
		
		addTypeValidator (FieldType.Object.toLowerCase (), 			new MapValidator ());
		addTypeValidator (FieldType.Array.toLowerCase (), 			new ArrayValidator ());
		
		addTypeValidator (FieldType.Stream.toLowerCase (), 			new StreamValidator ());
		addTypeValidator (FieldType.Base64.toLowerCase (), 			new Base64Validator ());
		addTypeValidator (FieldType.Binary.toLowerCase (), 			new BinaryValidator ());
		
		// vtypes
		addTypeValidator (FieldType.Facets.StartsWith.toLowerCase (),new StartsWithValidator ());
		addTypeValidator (FieldType.Facets.EndsWith.toLowerCase (), new EndsWithValidator ());
		addTypeValidator (FieldType.Facets.Contains.toLowerCase (), new ContainsValidator ());

		addTypeValidator (FieldType.Facets.Email.toLowerCase (), 	new EmailValidator ());
		addTypeValidator (FieldType.Facets.Url.toLowerCase (), 		new UrlValidator ());
		addTypeValidator (FieldType.Facets.Phone.toLowerCase (), 	new PhoneValidator ());
		
		addTypeValidator (FieldType.Facets.Regex.toLowerCase (), 	new RegexValidator ());
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
				type = FieldType.String;
			}
			
			if (type.equalsIgnoreCase (FieldType.Raw)) {
				continue;
			}
			
			TypeValidator validator = getTypeValidator (type);
			if (validator == null) {
				/*
				if (feedback == null) {
					feedback = new JsonObject ();
				}
				feedback.set (name, ValidationUtils.feedback (
					null, spec, Spec.Type, 
					"type '" + type + "' not supported"
				));
				*/
				validator = getTypeValidator (FieldType.Object);
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

	@Override
	public boolean isCustomType (String type) {
		return !validators.containsKey (type);
	}

}

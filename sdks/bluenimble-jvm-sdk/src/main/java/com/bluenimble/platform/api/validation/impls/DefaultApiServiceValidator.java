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
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidatorException;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.TypeValidator;
import com.bluenimble.platform.api.validation.ValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.AppendValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.LowerCaseValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.NullifyIfEmptyValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.PrependValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.ReplaceValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.TruncateValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.UpperCaseValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.MultiplyValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.DivideValueTransformer;
import com.bluenimble.platform.api.validation.impls.transformers.ModuloValueTransformer;
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
import com.bluenimble.platform.api.validation.impls.types.UUIDValidator;
import com.bluenimble.platform.api.validation.impls.types.UrlValidator;
import com.bluenimble.platform.json.JsonArray;
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
	
	private Map<String, TypeValidator> validators 		= new HashMap<String, TypeValidator> ();
	
	private Map<String, ValueTransformer> transformers 	= new HashMap<String, ValueTransformer> ();
	
	public DefaultApiServiceValidator () {
		// transformers
		addValueTransformer (ValueTransformer.Default.LowerCase, 	new LowerCaseValueTransformer ());
		addValueTransformer (ValueTransformer.Default.UpperCase, 	new UpperCaseValueTransformer ());
		addValueTransformer (ValueTransformer.Default.Append, 		new AppendValueTransformer ());
		addValueTransformer (ValueTransformer.Default.Prepend, 		new PrependValueTransformer ());
		addValueTransformer (ValueTransformer.Default.Truncate, 	new TruncateValueTransformer ());
		addValueTransformer (ValueTransformer.Default.Replace, 		new ReplaceValueTransformer ());
		addValueTransformer (ValueTransformer.Default.Nullify, 		new NullifyIfEmptyValueTransformer ());
		addValueTransformer (ValueTransformer.Default.Multiply, 	new MultiplyValueTransformer ());
		addValueTransformer (ValueTransformer.Default.Divide, 		new DivideValueTransformer ());
		addValueTransformer (ValueTransformer.Default.Modulo, 		new ModuloValueTransformer ());

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
		
		addTypeValidator (FieldType.UUID.toLowerCase (), 			new UUIDValidator ());
		
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
	
	public void validate (Api api, JsonObject spec, ApiConsumer consumer, ApiRequest request, Map<String, Object> data) 
		throws ApiServiceValidatorException {
		
		String refServiceId = null;
		String refField = null;
		
		String refSpec = Json.getString (spec, Spec.RefSpec);
		if (!Lang.isNullOrEmpty (refSpec)) {
			refSpec = refSpec.trim ();
			int indexOfSharp = refSpec.indexOf (Lang.SHARP);
			if (indexOfSharp > 0) {
				refServiceId 	= refSpec.substring (0, indexOfSharp).trim ();
				refField 		= refSpec.substring (indexOfSharp + 1).trim ();
			}
		}
		
		if (!Lang.isNullOrEmpty (refServiceId)) {
			ApiService refService = api.getServicesManager ().getById (refServiceId);
			if (refService != null && !Json.isNullOrEmpty (refService.getSpecification ())) {
				JsonObject oRefSpec = refService.getSpecification ();
				if (!Lang.isNullOrEmpty (refField)) {
					refField = Spec.Fields + Lang.DOT + refField;
					Object fieldSpec = Json.find (oRefSpec, Lang.split (refField, Lang.DOT));
					if (fieldSpec != null && (fieldSpec instanceof JsonObject)) {
						oRefSpec = (JsonObject)fieldSpec;
					}
				}
				oRefSpec.duplicate ().merge (spec);
				spec = oRefSpec;
			}
		}

		if (Json.isNullOrEmpty (spec)) {
			return;
		}
		
		JsonObject oFields = Json.getObject (spec, Spec.Fields);
		JsonObject altSpec = 
			Json.getObject (
				Json.getObject (spec, Spec.Selectors),
				(String)request.get (ApiRequest.SelectSpec)
			);
		
		if (Json.isNullOrEmpty (oFields)) {
			// any alternative spec
			if (!Json.isNullOrEmpty (altSpec)) {
				validate (api, altSpec, consumer, request, data);
			}
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
			
			String type = Json.getString (fSpec, Spec.Type, FieldType.String);
			
			if (type.equalsIgnoreCase (FieldType.Raw)) {
				continue;
			}
			
			TypeValidator validator = getTypeValidator (type);
			if (validator == null) {
				validator = getTypeValidator (FieldType.Object);
			}
			
			String label = getLabel (name, fSpec.getString (Spec.Title));
			
			Object value = valueOf (name, fSpec, request, consumer, data);
			
			value = transform (fSpec, value, 0);
			
			Object message = validator.validate (
				api,
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
					message = transform (fSpec, message, 1);
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
			ApiServiceValidatorException vex = new ApiServiceValidatorException (feedback);
			ApiResponse.Status status = (ApiResponse.Status)request.get (ApiRequest.ResponseStatus);
			if (status != null) {
				vex.status (status);
			}
			throw vex;
		}
		
		// any alternative spec
		if (!Json.isNullOrEmpty (altSpec)) {
			validate (api, altSpec, consumer, request, data);
		}
		
	}

	private Object transform (JsonObject fSpec, Object value, int timing) {
		JsonArray transforms = Json.getArray (fSpec, Spec.Transforms);
		if (Json.isNullOrEmpty (transforms)) {
			return value;
		}
		for (int i = 0; i < transforms.count (); i++) {
			JsonObject oTransform = (JsonObject)transforms.get (i);
			String transformName = Json.getString (oTransform, Spec.Name);
			if (Json.getInteger (oTransform, ValueTransformer.Timing, 0) != timing) {
				continue;
			}
			ValueTransformer t = getValueTransformer (transformName);
			if (t == null) {
				continue;
			}
			value = t.transform (this, Json.getObject (oTransform, ValueTransformer.Spec), value);
		}
		return value;
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
		
		Object defaultValue = fieldSpec.get (Spec.Value);
		
		if (data != null) {
			Object dValue = data.get (name);
			if (dValue == null) {
				dValue = defaultValue;
			}
			return dValue;
		}
		
		Object value = null;
		
		String s = fieldSpec.getString (Spec.Scope);
		if (Lang.isNullOrEmpty (s)) {
			s = DefaultScope;
		}
		
		Scope scope = null;
		s = s.trim ();
		for (int i = 0; i < s.length (); i++) {
			char sc = s.charAt (i);
			if (ConsumerScope == sc) {
				if (defaultValue != null) {
					value = getFromConsumer (consumer, defaultValue.toString ());
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
		}
		
		if (value != null) {
			request.set (name, value);
		}
		
		return value;
		
	}

	@Override
	public boolean isCustomType (String type) {
		if (type == null) {
			return false;
		}
		return !validators.containsKey (type.toLowerCase ());
	}
	
	private Object getFromConsumer (ApiConsumer consumer, String property) {
		if (Lang.isNullOrEmpty (property)) {
			return null;
		}
		
		String [] accessors = Lang.split (property, Lang.DOT);
		
		Object value = consumer.get (accessors [0]);

		if (accessors.length == 1) {
			return value;
		}
		
		if (!(value instanceof JsonObject)) {
			return null;
		}
		
		return Json.find ((JsonObject)value, Lang.moveLeft (accessors, 1));
	}

	@Override
	public void addValueTransformer (String name, ValueTransformer validator) {
		transformers.put (name.toLowerCase (), validator);
	}

	@Override
	public ValueTransformer getValueTransformer (String name) {
		return transformers.get (name.toLowerCase ());
	}

}

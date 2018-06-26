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

package com.bluenimble.platform.apis.mgm.media;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiAccessDeniedException;
import com.bluenimble.platform.api.ApiServicesManager;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class TemplateTool {
	
	private Api 				mgm;
	
	private static final String OasGenericObject = "Object_";
	
	private static final String OasString 	= "string";
	private static final String OasObject 	= "object";
	private static final String OasArray 	= "array";
	
	public static final String OasPlaceholder 	
											= "placeholder";
	
	private static final Map<String, String []> OasTypes = new HashMap<String, String []> ();
	static {
		OasTypes.put (FieldType.Integer.toLowerCase(),				new String [] { "integer", "int32" });
		OasTypes.put (FieldType.Long.toLowerCase(),					new String [] { "integer", "int64" });
		OasTypes.put (FieldType.Float.toLowerCase(), 				new String [] { "number", "float" });
		OasTypes.put (FieldType.Decimal.toLowerCase(), 				new String [] { "number", "double" });
		OasTypes.put (FieldType.String.toLowerCase(), 				new String [] { OasString });
		OasTypes.put (FieldType.AlphaNumeric.toLowerCase(), 		new String [] { OasString }); // need review
		OasTypes.put (FieldType.Base64.toLowerCase(), 				new String [] { OasString, "byte" });
		OasTypes.put (FieldType.Binary.toLowerCase(), 				new String [] { OasString, "binary" });
		OasTypes.put (FieldType.Boolean.toLowerCase(), 				new String [] { "boolean" });
		OasTypes.put (FieldType.Date.toLowerCase(), 				new String [] { OasString, "date" });
		OasTypes.put (FieldType.DateTime.toLowerCase(), 			new String [] { OasString, "date-time" });
		OasTypes.put (ApiServiceValidator.Spec.Secret, 				new String [] { OasString, "password" });
	}
	
	public interface OasSpec {
		interface Schema {
			String Properties 		= "properties";
			String Ref 				= "$ref";
			String Items			= "items";
			
			String Required 		= "required";
			String Description		= "description";

			String Type 			= "type";
			String Format 			= "format";
			String MinLength 		= "minLength";
			String MaxLength 		= "maxLength";
			String Pattern 			= "pattern";
			String Minimum 			= "minimum";
			String Maximum 			= "maximum";
			String ExclusiveMinimum = "exclusiveMinimum";
			String ExclusiveMaximum = "exclusiveMaximum";
			String MultipleOf 		= "multipleOf";
			String Enum 			= "enum";
		}
		interface Placeholder {
			String Path 		= "path";
			String Query 		= "query";
			String Header 		= "header";
			String Body 		= "body";
			String Form 		= "form";
		}
	}
	
	interface GroupingFlowsKeys {
		String Oas = "oas";
	}
	
	private static final Map<String, Class<? extends ApiServicesManager.GroupingFlow>> GroupingFlows = 
			new HashMap<String, Class<? extends ApiServicesManager.GroupingFlow>> ();
	static {
		GroupingFlows.put (GroupingFlowsKeys.Oas, OasGroupingFlow.class);
	}
 	
	public void init (Api mgm) {
		this.mgm = mgm;
	}
	
	public List<Object> newList () {
		return new ArrayList<Object> ();
	} 
	
	public Map<String, Object> newMap () {
		return new HashMap<String, Object> ();
	} 
	
	public String log (Object value) {
		//System.out.println (value);
		return Lang.BLANK;
	} 
	
	public int addToList (List<Object> list, Object value) {
		list.add (value);
		return 0;
	} 
	
	public boolean isObjectType (String space, String api, JsonObject spec) throws ApiAccessDeniedException {
		return isObjectType (
			mgm.space ().space (space).api (api).getServiceValidator (), 
			space, api, spec
		);
	}
	
	private boolean isObjectType (ApiServiceValidator validator, String space, String api, JsonObject spec) throws ApiAccessDeniedException {
		String type = Json.getString (spec, Spec.Type);
		if (Lang.isNullOrEmpty (type)) {
			return false;
		}
		return FieldType.Object.equalsIgnoreCase (type) || validator.isCustomType (type);
	}
	
	public JsonObject groupBy (String space, String api, String property, String groupItemKey, String flow) throws Exception {
		return mgm.space ().space (space).api (api).getServicesManager ().groupBy (property, groupItemKey, GroupingFlows.get (flow).newInstance ());
	} 
	
	public String oasSchema (String space, String api, JsonObject spec, Map<String, Object> components) throws ApiAccessDeniedException {
		return jOasSchema (space, api, spec, components).toString (0);
	}
	
	@SuppressWarnings("unchecked")
	public JsonObject jOasSchema (String space, String api, JsonObject spec, Map<String, Object> components) throws ApiAccessDeniedException {
		
		if (isObjectType (space, api, spec) || FieldType.Array.equalsIgnoreCase (Json.getString(spec, Spec.Type))) {
			return oasObjectSchema (space, api, spec, components);
		}
		
		String type = Json.getString (spec, Spec.Type, FieldType.String).toLowerCase ();
		
		if (!OasTypes.containsKey (type)) {
			type = FieldType.String.toLowerCase ();
		}
		if (Json.getBoolean (spec, Spec.Secret, false)) {
			type = Spec.Secret;
		}
		
		String [] attrs = OasTypes.get (type);
		
		String oasType = attrs [0];
		
		JsonObject schema = new JsonObject ();

		schema.set (OasSpec.Schema.Type, oasType);
		if (attrs.length > 1) {
			schema.set (OasSpec.Schema.Format, attrs [1]);
		}
		
		if (spec.containsKey (Spec.VType)) {
			String vType = Json.getString (spec, Spec.VType).toLowerCase ();
			if (vType.equalsIgnoreCase (FieldType.Facets.Regex)) {
				schema.set (OasSpec.Schema.Pattern, Json.getString (spec, Spec.Format));
			} else {
				schema.set (OasSpec.Schema.Format, vType);
			}
		}
		
		String [] aFacet = getFacet (spec, Spec.Min);
		if (aFacet != null) {
			if (oasType.equals (OasString)) {
				schema.set (OasSpec.Schema.MinLength, aFacet [0]);
			} else {
				schema.set (OasSpec.Schema.Minimum, aFacet [0]);
				schema.set (OasSpec.Schema.ExclusiveMinimum, aFacet [1]);
			}
		}
		
		aFacet = getFacet (spec, Spec.Max);
		if (aFacet != null) {
			if (oasType.equals (OasString)) {
				schema.set (OasSpec.Schema.MaxLength, aFacet [0]);
			} else {
				schema.set (OasSpec.Schema.Maximum, aFacet [0]);
				schema.set (OasSpec.Schema.ExclusiveMaximum, aFacet [1]);
			}
		}
		
		if (spec.containsKey (Spec.Enum)) {
			
			Object _enum = spec.get (Spec.Enum);
			
			if (_enum instanceof JsonArray) {
				schema.set (OasSpec.Schema.Enum, _enum);
			} else if (_enum instanceof JsonObject) {
				schema.set (OasSpec.Schema.Enum, new ArrayList<String> (((JsonObject)_enum).keySet ()));
			}
		}
		
		return schema;
		
	}
	
	public JsonObject oasObjectSchema (String space, String api, JsonObject spec, Map<String, Object> components) throws ApiAccessDeniedException {
		
		JsonObject oasSchema = new JsonObject ();
		
		JsonObject oasProps = new JsonObject ();
		oasProps.set (OasSpec.Schema.Type, OasObject);
		oasSchema.set (OasSpec.Schema.Properties, oasProps);
		
		JsonArray oasRequired = new JsonArray ();
		oasSchema.set (OasSpec.Schema.Required, oasRequired);
		
		JsonObject schemaRef = new JsonObject ();
		
		JsonObject refTarget = schemaRef;

		String type = Json.getString (spec, Spec.Type);
		
		if (FieldType.Array.equalsIgnoreCase (type)) {
			schemaRef.set (OasSpec.Schema.Type, OasArray);
			type = Json.getString (spec, Spec.SType, FieldType.String);
			JsonObject oItems = new JsonObject ();
			schemaRef.set (OasSpec.Schema.Items, oItems);
			spec = spec.duplicate ();
			spec.set (Spec.Type, type);
			refTarget = oItems;
			if (!isObjectType (space, api, spec)) {
				schemaRef.set (
					OasSpec.Schema.Items, 
					jOasSchema (space, api, spec, components).set (OasSpec.Schema.Description, Json.getString (spec, Spec.Title))
				);
				return schemaRef;
			}
		}
		
		JsonObject fields = Json.getObject (spec, Spec.Fields);
		
		if (!Json.isNullOrEmpty (fields)) {
			Iterator<String> keys = fields.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				JsonObject oField = Json.getObject (fields, key);
				if (Json.getBoolean (oField, Spec.Required, true)) {
					oasRequired.add (key);
				}
				oasProps.set (
					key, 
					jOasSchema (space, api, oField, components)
						.set (OasSpec.Schema.Description, Json.getString (oField, Spec.Title)
					)			
				);
			}
		}
		
		if (oasRequired.isEmpty ()) {
			oasSchema.remove (OasSpec.Schema.Required);
		}
		
		if (FieldType.Object.equalsIgnoreCase (type)) {
			type = OasGenericObject + components.size ();
		}

		components.put (type, oasSchema.toString (0));
		
		refTarget.set (OasSpec.Schema.Ref, "#/components/schemas/" + type);
		
		return schemaRef;
	}
	
	private String [] getFacet (JsonObject spec, String facet) {
		if (!spec.containsKey (facet)) {
			return null;
		}
		
		String facetValue = Json.getString (spec, facet).trim ();
		if (!Lang.isNullOrEmpty (facetValue)) {
			return null;
		}
		
		String exclusive = Lang.FALSE;
		if (facetValue.startsWith (ApiServiceValidator.Spec.Exclusive)) {
			exclusive = Lang.TRUE;
			facetValue.substring (1);
		}
		
		return new String [] { facetValue, exclusive };
			
	}
	
	public String json2str (JsonObject object) {
		if (object == null) {
			return Lang.BLANK;
		}
		return object.toString (0);
	}
	
	public Object guess (String space, String api, String name, JsonObject spec) throws ApiAccessDeniedException {
		ApiServiceValidator validator = mgm.space ().space (space).api (api).getServiceValidator ();
		Object value = ValidationUtils.guessValue (validator, name, spec);
		if (value == null) {
			if (isObjectType (validator, space, api, spec)) {
				return new JsonObject ().toString (0);
			}
			return Lang.BLANK;
		}
		if (value instanceof JsonObject) {
			return ((JsonObject)value).toString (0);
		}
		return String.valueOf (value);
	}
	
}
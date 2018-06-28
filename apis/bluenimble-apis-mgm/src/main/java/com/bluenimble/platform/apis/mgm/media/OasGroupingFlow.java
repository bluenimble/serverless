package com.bluenimble.platform.apis.mgm.media;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiAccessDeniedException;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServicesManager;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.apis.mgm.media.TemplateTool.OasSpec;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class OasGroupingFlow implements ApiServicesManager.GroupingFlow {
		
	private static final String SlashColon = "/:";
	
	public interface Scope {
		String Header 	= "h";
		String Parameter = "p";
		String Stream 	= "s";
	}
	
	private static final Map<String, String> VerbAction = new HashMap<String, String>();
	static {
		VerbAction.put (ApiVerb.GET.name ().toUpperCase (), "Read");
		VerbAction.put (ApiVerb.POST.name ().toUpperCase (), "Create");
		VerbAction.put (ApiVerb.PUT.name ().toUpperCase (), "Update");
		VerbAction.put (ApiVerb.PATCH.name ().toUpperCase (), "Update");
		VerbAction.put (ApiVerb.DELETE.name ().toUpperCase (), "Delete");
	}
	
	private static final String GlobalOperation = "Global";
	
	private Set<String> pathFields = new HashSet<String> ();
	
	@Override
	public String onGroupKey (Api api, String groupKey) {
		
		pathFields.clear ();
		
		int indexOfSlashColon = groupKey.indexOf (SlashColon);
		if (indexOfSlashColon < 0) {
			return groupKey;
		}
		
		StringBuilder sb = new StringBuilder ();
		
		String [] elements = Lang.split (groupKey, Lang.SLASH);
		for (String e : elements) {
			boolean isParameter = false;
			if (e.startsWith (Lang.COLON)) {
				isParameter = true;
				e = e.substring (1);
			}
			// if /::path
			if (e.startsWith (Lang.COLON)) {
				isParameter = true;
				e = e.substring (1);
			}

			if (isParameter) {
				pathFields.add (e);
			}
			
			sb.append (Lang.SLASH);
			
			if (isParameter) {
				sb.append (Lang.OBJECT_OPEN);
			}
			
			sb.append (e);
			
			if (isParameter) {
				sb.append (Lang.OBJECT_CLOSE);
			}
			
		}
		
		groupKey = sb.toString ();
		sb.setLength (0);
		
		return groupKey;
	}
	@Override
	public JsonObject onService (Api api, JsonObject service, boolean isObject) {
		
		JsonObject spec = Json.getObject (service, ApiService.Spec.Spec);
		if (Json.isNullOrEmpty (spec)) {
			spec = new JsonObject ();
			service.set (ApiService.Spec.Spec, spec);
		}
		
		JsonObject oFields = Json.getObject (spec, ApiServiceValidator.Spec.Fields);
		if (Json.isNullOrEmpty (oFields)) {
			oFields = new JsonObject ();
			spec.set (ApiServiceValidator.Spec.Fields, oFields);
		}
		
		if (pathFields != null && !pathFields.isEmpty ()) {
			for (String pf : pathFields) {
				if (!oFields.containsKey (pf)) {
					oFields.set (pf, new JsonObject ());
				}
			}
		}
		
		String verb = Json.getString (service, ApiService.Spec.Verb, ApiVerb.GET.name ()).toUpperCase ();
		
		JsonObject meta = Json.getObject (service, ApiService.Spec.Meta.class.getSimpleName ().toLowerCase ());
		if (meta == null) {
			meta = new JsonObject ();
			service.set (ApiService.Spec.Meta.class.getSimpleName ().toLowerCase (), meta);
		}
		JsonArray tags = Json.getArray (meta, ApiService.Spec.Meta.Tags);
		if (tags == null) {
			tags = new JsonArray ();
			meta.set (ApiService.Spec.Meta.Tags, tags);
			String actionTag = VerbAction.get (verb);
			if (actionTag != null) {
				tags.add (actionTag);
			}
		}
		
		String serviceId = Json.getString (service, ApiService.Spec.Id);
		if (!Lang.isNullOrEmpty (serviceId)) {
			int indexOfDot = serviceId.lastIndexOf (Lang.DOT);
			if (indexOfDot < 0) {
				tags.add (GlobalOperation);
			} else {
				tags.add (serviceId.substring (0, indexOfDot).replace (Lang.DOT, Lang.BLANK));
			}
		}
		
		boolean isBodyAware = verb.equals (ApiVerb.POST.name ()) || verb.equals (ApiVerb.PUT.name ()) || verb.equals (ApiVerb.PATCH.name ());
		
		Iterator<String> keys = oFields.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			JsonObject field = Json.getObject (oFields, key);
			String scope = Json.getString (field, ApiServiceValidator.Spec.Scope, Scope.Parameter);
			if (Scope.Header.equalsIgnoreCase (scope)) {
				field.set (TemplateTool.OasPlaceholder, OasSpec.Placeholder.Header);
			} else if (Scope.Stream.equalsIgnoreCase (scope)) {
				field.set (TemplateTool.OasPlaceholder, OasSpec.Placeholder.Body);
			} else if (Scope.Parameter.equalsIgnoreCase (scope)) {
				if (pathFields.contains (key)) {
					field.set (TemplateTool.OasPlaceholder, OasSpec.Placeholder.Path);
				} else if (isBodyAware) {
					try {
						if (isObjectType (api, field)) {
							field.set (TemplateTool.OasPlaceholder, OasSpec.Placeholder.Body);
						} else {
							field.set (TemplateTool.OasPlaceholder, OasSpec.Placeholder.Form);
						}
					} catch (ApiAccessDeniedException ex) {
						throw new RuntimeException (ex.getMessage (), ex);
					}
				} else {
					field.set (TemplateTool.OasPlaceholder, OasSpec.Placeholder.Query);
				}
			}
		}
		
		return service;
	}
	
	private boolean isObjectType (Api api, JsonObject spec) throws ApiAccessDeniedException {
		String type = Json.getString (spec, Spec.Type);
		if (Lang.isNullOrEmpty (type)) {
			return false;
		}
		return FieldType.Object.equalsIgnoreCase (type) || api.getServiceValidator ().isCustomType (type);
	}
	
}
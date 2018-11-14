package com.bluenimble.platform.templating.impls;

import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.templating.VariableResolver;

public class BasicVariableResolver implements VariableResolver {
	
	private static final long serialVersionUID = -485939153491337463L;
	
	private JsonObject data;
	
	public BasicVariableResolver (JsonObject data) {
		this.data = data;
	}
	
	@Override
	public Object resolve (String namespace, String... property) {
		if (Lang.isNullOrEmpty (namespace)) {
			return Json.find ((JsonObject)data, property);
		}
		Object root = data.get (namespace);
		if (property == null || property.length == 0) {
			return root;
		}
		if (!(root instanceof JsonObject)) {
			return null;
		}
		return Json.find ((JsonObject)root, property);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> bindings () {
		return data;
	}

}

package com.bluenimble.platform.api.validation.impls.transformers;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonObject;

import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ValueTransformer;

public class ModuloValueTransformer implements ValueTransformer {

	@Override
	public Object transform (ApiServiceValidator validator, JsonObject spec, Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof Integer || object.getClass ().equals (Integer.TYPE)) {
			return (Integer)object % Json.getInteger (spec, Value, 1);
		} else if (object instanceof Long || object.getClass ().equals (Long.TYPE)) {
			return (Long)object % Json.getLong (spec, Value, 1);
		} else if (object instanceof Double || object.getClass ().equals (Double.TYPE)) {
			return (Double)object % Json.getDouble (spec, Value, 1);
		}
		return object;
	}

}

package com.bluenimble.platform.api.validation.impls.transformers;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ValueTransformer;
import com.bluenimble.platform.json.JsonObject;

public class PrependValueTransformer implements ValueTransformer {

	@Override
	public Object transform (ApiServiceValidator validator, JsonObject spec, Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof String) {
			return Json.getString (spec, Value, Lang.BLANK) + String.valueOf (object);
		} else if (object instanceof Integer) {
			return (Integer)object + Json.getInteger (spec, Value, 0);
		} else if (object instanceof Long) {
			return (Long)object + Json.getLong (spec, Value, 0);
		} else if (object instanceof Double) {
			return (Double)object + Json.getDouble (spec, Value, 0);
		}
		
		return object;
	}

}

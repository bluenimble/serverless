package com.bluenimble.platform.api.validation.impls.transformers;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ValueTransformer;
import com.bluenimble.platform.json.JsonObject;

public class NullifyIfEmptyValueTransformer implements ValueTransformer {

	@Override
	public Object transform (ApiServiceValidator validator, JsonObject spec, Object object) {
		if (object == null) {
			return null;
		}
		String str = String.valueOf (object);
		if (Lang.isNullOrEmpty (str)) {
			return null;
		}
		return object;
	}

}

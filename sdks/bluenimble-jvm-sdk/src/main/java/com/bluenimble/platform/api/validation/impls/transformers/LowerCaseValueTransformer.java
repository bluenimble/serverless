package com.bluenimble.platform.api.validation.impls.transformers;

import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ValueTransformer;
import com.bluenimble.platform.json.JsonObject;

public class LowerCaseValueTransformer implements ValueTransformer {

	@Override
	public Object transform (ApiServiceValidator validator, JsonObject spec, Object object) {
		if (object == null) {
			return null;
		}
		return String.valueOf (object).toLowerCase ();
	}

}

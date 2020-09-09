package com.bluenimble.platform.api.validation.impls.transformers;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ValueTransformer;
import com.bluenimble.platform.json.JsonObject;

public class ReplaceValueTransformer implements ValueTransformer {

	interface SpecExt {
		String Old 	= "old";
		String New 	= "new";
	}
	
	@Override
	public Object transform (ApiServiceValidator validator, JsonObject spec, Object object) {
		if (object == null) {
			return null;
		}
		
		return Lang.replace (String.valueOf (object), Json.getString (spec, SpecExt.Old), Json.getString (spec, SpecExt.New));
	}

}

package com.bluenimble.platform.api.validation.impls.transformers;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ValueTransformer;
import com.bluenimble.platform.json.JsonObject;

public class TruncateValueTransformer implements ValueTransformer {

	interface Spec {
		String Start 	= "start";
		String End 		= "end";
	}
	
	@Override
	public Object transform (ApiServiceValidator validator, JsonObject spec, Object object) {
		if (object == null) {
			return null;
		}
		String value = String.valueOf (object);
		
		int start 	= Json.getInteger (spec, Spec.Start, 0);
		int end 	= Json.getInteger (spec, Spec.End, value.length ());
		
		return value.substring (start, end);
	}

}

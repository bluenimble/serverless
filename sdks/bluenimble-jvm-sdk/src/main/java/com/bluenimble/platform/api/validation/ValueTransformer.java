package com.bluenimble.platform.api.validation;

import com.bluenimble.platform.json.JsonObject;

public interface ValueTransformer {
	
	interface Default {
		String LowerCase = "lowercase";
		String UpperCase = "uppercase";
		String Append = "append";
		String Prepend = "prepend";
		String Replace = "replace";
		String Truncate = "truncate";
		String Nullify = "nullify";
	}

	Object transform (ApiServiceValidator validator, JsonObject spec, Object object); 
	
}

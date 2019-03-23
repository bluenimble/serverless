package com.bluenimble.platform.api.validation;

import com.bluenimble.platform.json.JsonObject;

public interface ValueTransformer {
	
	interface Default {
		String LowerCase = "lowercase";
		String UpperCase = "uppercase";
		String Append = "uppercase";
		String Prepend = "uppercase";
		String Replace = "uppercase";
		String Truncate = "uppercase";
	}

	Object transform (ApiServiceValidator validator, JsonObject spec, Object object); 
	
}

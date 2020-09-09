package com.bluenimble.platform.api.validation;

import com.bluenimble.platform.json.JsonObject;

public interface ValueTransformer {
	
	String Timing		= "timing";
	String Spec			= "spec";
	String Value		= "value";

	interface Default {
		String LowerCase 	= "lowercase";
		String UpperCase 	= "uppercase";
		String Append 		= "append";
		String Prepend 		= "prepend";
		String Replace 		= "replace";
		String Truncate 	= "truncate";
		String Nullify 		= "nullify";
		String Multiply 	= "multiply";
		String Divide 		= "divide";
		String Modulo			= "modulo";
	}

	Object transform (ApiServiceValidator validator, JsonObject spec, Object object); 
	
}

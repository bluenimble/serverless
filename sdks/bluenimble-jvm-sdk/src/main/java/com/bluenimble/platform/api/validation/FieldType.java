package com.bluenimble.platform.api.validation;

public interface FieldType {

	String String 			= "String";
	String AlphaNumeric 	= "AlphaNumeric";
	
	String Integer 			= "Integer";
	String Long 			= "Long";

	String Decimal 			= "Decimal";
	String Float 			= "Float";
	
	String Boolean 			= "Boolean";
	
	String Date 			= "Date";
	String DateTime 		= "DateTime";
	
	String Object 			= "Object";
	
	String Array 			= "Array";
	
	String Stream			= "Stream";
	String Binary			= "Binary";
	String Base64			= "Base64";
	
	String UUID 			= "UUID";

	String Raw				= "Raw";
	
	interface Facets {
		String Url 			= "Url";
		String Email 		= "Email";
		String Phone 		= "Phone";
		String Regex 		= "Regex";
		
		String StartsWith 	= "StartsWith";
		String EndsWith		= "EndsWith";
		String Contains 	= "Contains";
	}
	
}

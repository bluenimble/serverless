/**
  Hex Hashing Utility Class
  @namespace Hex
*/
var Hex = {
	/**	
	  Encodes a string into hexadecimal 
	  @param {string} - the data to encode
	  
	  @returns {string} the encoded data
	*/
	encode: function (data) {
		return new JC_String (JC_Lang.encodeHex (data.getBytes ()));
	}, 
	/**	
	  Decodes a string from hexadecimal 
	  @param {string} data - the data to decode
	  
	  @returns {string} the decoded data
	*/
	decode: function (data) {
		return new JC_String (JC_Lang.decodeHex (data.toCharArray ()));
	}
};
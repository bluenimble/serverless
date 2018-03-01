/**
  Base64 Hashing Utility Class
  @namespace Base64
*/
var Base64 = {
	/**	
	  Encodes a string into base64 
	  @param {string|ByteArray} - the data to encode
	  
	  @returns {string} the encoded base64
	*/
	encode: function (data) {
		return JC_Base64.encodeBase64 (data);
	}, 
	/**	
	  Decodes a base64 string
	  @param {string|ByteArray} - the base64String to decode
	  
	  @returns {ByteArray} the decoded string
	*/
	decode: function (data) {
		return JC_Base64.decodeBase64 (data);
	}
};
/**
  Hex Hashing Utility Class
  @namespace Hex
*/
var Hex = {
	/**	
	  Encodes a string into hexadecimal 
	  @param {ByteArray} - the data to encode
	  
	  @returns {string} the encoded data
	*/
	encode: function (data) {
		return new JC_String (JC_Lang.encodeHex (data));
	}, 
	/**	
	  Decodes a string from hexadecimal 
	  @param {string} data - the data to decode
	  
	  @returns {ByteArray} the decoded data
	*/
	decode: function (data) {
		return JC_Lang.decodeHex (data.toCharArray ());
	}
};
/**
  Lang Utility Class
  @namespace Lang
*/
var Lang = {
	/**	
	  Get the bytes of a string 
	  @param {string} - the string  
	  @param {string} [charset] - the charset to use
	  @returns {byte[]} the byte array
	*/
	toBytes: function (str, charset) {
		if (!str) {
			return;
		}
		if (!charset) {
			return str.getBytes ();
		}
		return str.getBytes (charset);
	},
	/**	
	  Create a string from a bytes array
	  @param {byte[]} - the string  
	  @param {string} [charset] - the charset to use
	  @returns {string} the byte array
	*/
	fromBytes: function (bytes, charset) {
		if (!bytes) {
			return;
		}
		if (!charset) {
			return new JC_String (bytes);
		}
		return new JC_String (bytes, charset);
	},
	/**	
	  Convert a long number to a bytes array
	  @param {byte[]} - bytes array
	  @returns a long number
	*/
	longToBytes: function (x) {
        return JC_Lang.longToBytes (x);
    },
	/**	
	  Convert a byte array to a long number
	  @param {long} - value
	  @returns the byte array
	*/
    bytesToLong: function (bytes) {
        return JC_Lang.bytesToLong (bytes);
    },
	/**	
	  Generates a random hex based object id 
	  @returns {string} the oid
	*/
	oid: function () {
		return JC_Lang.oid ();
	},
	/**	
	  Generates a random hex based guid 
	  @returns {string} the guid
	*/
	guid: function () {
		return JC_Lang.rand ();
	},
	/**	
	  Generates a random string of a specific length
	  @param {integer} [length=10] - the length of the random string  

	  @returns {string} the random string
	*/
	rand: function (len) {
		if (!len) {
			len = 10;
		}
		return JC_Lang.UUID (len);
	},
	/**	
	  Generates a random pin number of a specific length
	  @param {integer} [length=4] - the length of the pin

	  @returns {integer} the pin
	*/
	pin: function (len) {
		if (!len) {
			len = 4;
		}
		return JC_Lang.pin (len);
	},
	/**	
	  Check if the value in argument is an array
	  @param {Object} - the target array 

	  @returns {boolean} true if it's an array
	*/
	isArray: function (o) {
		return (o instanceof JC_JsonArray) || Object.prototype.toString.call (o) === '[object Array]';
	},
	/**	
	  Check if the value in argument is an object
	  @param {Object} - the target object

	  @returns {boolean} true if it's an object
	*/
	isObject: function (o) {
		if (!o) {
			return false;
		}
		return (o instanceof JC_JsonObject) || Object.prototype.toString.call (o) === '[object Object]';
	},
	
	/**	
	  Check if the value in argument is a string
	  @param {Object} - the target object

	  @returns {boolean} true if it's an object
	*/
	isString: function (o) {
		if (!o) {
			return false;
		}
		return typeof o === 'string' || o instanceof String;
	},
	
	/**	
	  Check if the value in argument is a number
	  @param {Object} - the target object

	  @returns {boolean} true if it's a number
	*/
	isNumber: function (o) {
		if (!o) {
			return false;
		}
		return typeof o === 'number' || o instanceof Number;
	},
	
	/**	
	  Check if the value in argument is an integer
	  @param {Object} - the target object

	  @returns {boolean} true if it's an integer
	*/
	isInteger: function (o) {
		return Lang.isNumber (o) && (o % 1) === 0;
	},
	
	/**	
	  Check if the value in argument is a boolean
	  @param {Object} - the target object

	  @returns {boolean} true if it's a boolean
	*/
	isBoolean: function (o) {
		if (typeof o === 'undefined') {
			return false;
		}
		return typeof o === 'boolean' || o instanceof Boolean;
	},
	
	/**	
	  Check if the value in argument is a boolean
	  @param {Object} - the target object

	  @returns {boolean} true if it's a boolean
	*/
	isDate: function (o) {
		if (typeof o === 'undefined') {
			return false;
		}
		return typeof o === 'date' || o instanceof Date;
	},
	
	/**	
	  Create a key pair
	*/
	keys: function () {
		return JC_Lang.keys ();
	},
	
	/**	
	  get an integer from the given string
	*/
	toInteger: function (value) {
		return JC_Integer.valueOf (value);
	},
	
	/**	
	  get an double from the given string
	*/
	toDouble: function (value) {
		return JC_Double.valueOf (value);
	},
	
	/**	
	  replace a portion of a string with a new one in the entire source string
	  @param {string} - the source sring
	  @param {string} - the string to replace
	  @param {string} - the new replacement string

	  @returns {string} 

	*/
	replace: function (source, os, ns) {
		return JC_Lang.replace (source, os, ns);
	},
	
	/**	
	  Template a string with data provided in argument
	  @param {string} - the template
	  @param {Object} - the data
	  @returns {string}
	*/
	template: function (template, data, withScripting) {
		if (typeof withScripting == 'undefined') {
			withScripting = false;
		}
		return JC_Lang.template (template, JC_ValueConverter.convert (data), withScripting);
	}
	
};
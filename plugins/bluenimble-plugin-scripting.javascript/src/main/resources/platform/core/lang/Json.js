/**
  Json Utility Class
  @namespace Json
*/
var Json = {
	/**	
	  Parses a string into a json object 
	  @param {string} - well-formed json string
	  
	  @returns {JsonObject} the json object
	*/
	parse: function (data) {
		return new JC_JsonObject (data);
	},
	
	/**	
	  create a json object 
	  
	  @returns {JsonObject} the json object
	*/
	object: function (data) {
		if (!data) {
			return new JC_JsonObject ();
		}
		return JC_ValueConverter.convert (data);
	},
	
	/**	
	  create a json array
	  
	  @returns {JsonArray} the json array
	*/
	array: function (data) {
		if (!data) {
			return new JC_JsonArray ();
		}
		return JC_ValueConverter.convert (data);
	},
	
	merge: function (root, extra) {
		var merged = JC_ValueConverter.convert (root);
		merged.merge (JC_ValueConverter.convert (extra));
		return merged;
	},
	
	duplicate: function (data) {
		var data = JC_ValueConverter.convert (data);
		return data.duplicate ();
	},
	
	integer: function (data, property, defaultValue) {
		if (typeof defaultValue == 'undefined') {
			defaultValue = 0;
		}
		return JC_Json.getInteger (JC_ValueConverter.convert (data), property, defaultValue);
	}, 
	
	boolean: function (data, property, defaultValue) {
		if (typeof defaultValue == 'undefined') {
			defaultValue = true;
		}
		return JC_Json.getBoolean (JC_ValueConverter.convert (data), property, defaultValue);
	}, 
	
	double: function (data, property, defaultValue) {
		if (typeof defaultValue == 'undefined') {
			defaultValue = 0;
		}
		return JC_Json.getDouble (JC_ValueConverter.convert (data), property, defaultValue);
	}, 
	
	date: function (data, property) {
		return JC_Json.getDate (JC_ValueConverter.convert (data), property);
	}, 

	sort: function (array, comapartor) {
		var JComparator = Java.extend (JC_Comparator, {
			compare: function (o1, o2) {
				return comapartor (o1, o2);
			}
		});
		array.sort (new JComparator ());
	}, 

	forEach: function (array, filter) {
		var JFilter = Java.extend (JC_JsonArrayFilter, {
			visit: function (object) {
				return filter (object);
			}
		});
		array.forEach (new JFilter ());
	}, 
	
	template: function (model, data, withScripting) {
		if (typeof withScripting == 'undefined') {
			withScripting = false;
		}
		return JC_Json.template (JC_ValueConverter.convert (model), JC_ValueConverter.convert (data), withScripting);
	}
	
};
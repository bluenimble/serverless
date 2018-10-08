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
		var jd = JC_Converters.convert (data);
		var o = new JC_JsonObject ();
		if (jd) {
			o.putAll (jd);
		}
		return o;
	},
	
	/**	
	  create a json array
	  
	  @returns {JsonArray} the json array
	*/
	array: function () {
		return new JC_JsonArray ();
	},
	
	merge: function (root, extra) {
		var merged = JC_Converters.convert (root);
		merged.merge (JC_Converters.convert (extra));
		return merged;
	},
	
	duplicate: function (data) {
		var data = JC_Converters.convert (data);
		return data.duplicate ();
	},
	
	integer: function (data, property, defaultValue) {
		if (typeof defaultValue == 'undefined') {
			defaultValue = 0;
		}
		return JC_Json.getInteger (JC_Converters.convert (data), property, defaultValue);
	}, 
	
	boolean: function (data, property, defaultValue) {
		if (typeof defaultValue == 'undefined') {
			defaultValue = true;
		}
		return JC_Json.getBoolean (JC_Converters.convert (data), property, defaultValue);
	}, 
	
	double: function (data, property, defaultValue) {
		if (typeof defaultValue == 'undefined') {
			defaultValue = 0;
		}
		return JC_Json.getDouble (JC_Converters.convert (data), property, defaultValue);
	}, 
	
	date: function (data, property) {
		return JC_Json.getDate (JC_Converters.convert (data), property);
	}, 
	
	template: function (model, data) {
		return JC_Json.template (JC_Converters.convert (model), JC_Converters.convert (data));
	}
	
};
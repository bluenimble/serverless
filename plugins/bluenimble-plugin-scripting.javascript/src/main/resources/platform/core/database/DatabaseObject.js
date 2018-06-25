/**
  Represents a database object<br/>
  <strong>Do not call constructor directly</strong><br/>
  @see Database
  @class DatabaseObject
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var DatabaseObject = function (database, proxy) {
	
	this.clazz 		= 'DatabaseObject';
	
	this.proxy 		= proxy;
	
	/**	
	  The database object id
	  @type {object}
	  @readonly
	*/
	this.id 		= proxy.getId ();
	/**	
	  The database object timestamp. When it was created or updated
	  @type {Date}
	  @readonly
	*/
	this.timestamp 	= proxy.getTimestamp ();
	
	/**	
	  Load data into this database object
	  @param {JsonObject} [values] - key/value data to load

	  @returns {DatabaseObject} this database object
	*/
	this.load 		= function (values) {
		if (!values) {
			return this;
		}
		this.proxy.load (JC_Converters.convert (values));
		return this;
	};
	
	/**	
	  Clear all data in this database object to be reused
	  @returns {DatabaseObject} this database object
	*/
	this.clear 		= function () {
		this.proxy.clear ();
		return this;
	};
	
	/**	
	  Test if a specific is in this database object
	  @returns {boolean} true or false
	*/
	this.has 		= function (key) {
		return this.proxy.has (key);
	};
	
	/**	
	  Use default assigned values auto uuid and timestamp.
	  If set to false, id and timestamp will not be added to the database object
	  @returns {DatabaseObject} this database object
	*/
	this.useDefaultFields 	= function (useDefaultFields) {
		this.proxy.useDefaultFields (useDefaultFields);
		return this;
	};
	
	/**	
	  Get a this database object data as Json
	  @param {function} - a function to filter which key/value pairs to serialize (optional)
	  
	  @returns {JsonObject} the database object as a json object
	*/
	this.toJson		= function (allStopLevel, minStopLevel) {
		var serializer;
		if (typeof allStopLevel == 'undefined' && typeof minStopLevel == 'undefined') {
			serializer = JC_BeanSerializer.Default;
		} else if (typeof minStopLevel == 'undefined') {
			serializer = new JC_DefaultBeanSerializer (allStopLevel, allStopLevel);
		} else {
			serializer = new JC_DefaultBeanSerializer (allStopLevel, minStopLevel);
		}
		return proxy.toJson (serializer);
	};
	
	/**	
	  Set a field value
	  @param {string} - the field name
	  @param {object} - the field value
	  
	  @returns {DatabaseObject} the database object
	*/
	this.set = function (key, value) {
		if (!key || !value) {
			return this;
		}
		
		// date object
		if (Lang.isDate (value)) {
			value = DateTime.withTimestamp (value.getTime ());
		}
		
		if (value.proxy && value.clazz == 'DatabaseObject') {
			value = value.proxy;
		} else if (value.clazz && value.clazz == 'LocalDateTime') {
			value = value.proxy;
		} else {
			value = JC_Converters.convert (value);
		} 
		
		proxy.set (key, value);
	
		return this;
	};
	
	/**	
	  Set a 1-1 field value
	  @param {string} - the field name
	  @param {string} - the reference entity name
	  @param {object} - the reference id
	  
	  @returns {DatabaseObject} the database object
	*/
	this.ref		= function (key, entity, id) {
		var ref = { };
		ref [JC_Database_Fields.Entity] = entity;
		ref [JC_Database_Fields.Id] = id;
		return this.set (key, ref);
	};
	
	/**	
	  Get a field value
	  @param {string} - the field name
	  
	  @returns {Object} the property value
	*/
	this.get		= function (key) {
		var value = proxy.get (key);
		if (!value) {
			return null;
		}
		if (JC_Converters.isDate (value)) {
			return new LocalDateTime ( JC_LocalDateTime.ofInstant (value.toInstant (), JC_ZoneId.systemDefault ()) );
		} else if (database.proxy.isEntity (value)) {
			return new DatabaseObject (database, value);
		}
		return value;
	};

	/**	
	  Delete this object from the database
	*/
	this.delete	= function () {
		proxy.delete ();
	};

	/**	
	  Remove a field from this object
	  @param {string} - the field name
	  
	  @returns {DatabaseObject} this database object
	*/
	this.remove		= function (key) {
		proxy.remove (key);
		return this;
	};

	/**	
	  List all object fields names 
	  
	  @returns {Array} fields names
	*/
	this.keys 		= function keys () {
		var keys = [];
		var iKeys = proxy.keys ();
		if (!iKeys) {
			return keys;
		}
		while (iKeys.hasNext ()) {
			keys.push (iKeys.next ());
		}
		return keys;
	}
	
	/**	
	  Save this database object to reflect changes made to it
	  
	  @returns {DatabaseObject} this database object
	*/
	this.save		= function () {
		proxy.save ();
		this.id = proxy.getId ();
		return this;
	};
	
	/**	
	  Increment the value of an integer property in this database object.<br/>
	  To decremt, use a negative value
	  @param {string} - field name
	  @param {integer} - the increment
	  
	  @returns {integer} the new value
	*/
	this.increment = function (field, value) {
		return database.proxy.increment (this.proxy, field, value);
	};
	
};
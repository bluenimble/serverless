/**
  Represents a database list of objects<br/>
  <strong>Do not call constructor directly</strong><br/>
  @see Database
  @class DatabaseObjectList
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var DatabaseObjectList = function (database, proxy) {
	
	this.clazz 		= 'DatabaseObjectList';
	
	this.proxy 		= proxy;
	
	/**	
	  Clear all this list
	  @returns {DatabaseObjectList} this database object list
	*/
	this.clear 		= function () {
		this.proxy.clear ();
		return this;
	};
	
	/**	
	  Get the list size
	  @returns {DatabaseObjectList} this database object list
	*/
	this.size 		= function () {
		return this.proxy.size ();
	};
	
	/**	
	  Insert a database object at a specific position
	  @param {number} - the index position
	  @param {DatabaseObject} - the database object
	  
	  @returns {DatabaseObjectList} the database object list
	*/
	this.set = function (index, dbo) {
		if ((typeof index == 'undefined') || !dbo) {
			return this;
		}
		proxy.set (index, dbo.proxy);
		return this;
	};
	
	/**	
	  Add a database object to the list
	  @param {DatabaseObject} - the database object
	  
	  @returns {DatabaseObjectList} the database object list
	*/
	this.add = function (dbo) {
		if (!dbo) {
			return this;
		}
		proxy.add (dbo.proxy);
		return this;
	};
	
	/**	
	  Get the object at the given index position
	  @param {number} - the index
	  
	  @returns {DatabaseObject} the database object at the requested position
	*/
	this.get = function (index) {
		return new DatabaseObject (database, proxy.get (index));
	};
	
	/**	
	  Remove an object from the list either by index or the object reference
	  @param {number|DatabaseObject} - the index or object
	  
	  @returns {DatabaseObjectList} the database object list
	*/
	this.remove = function (indexOrObject) {
		if (typeof indexOrObject == 'undefined') {
			return this;
		}
		if (Lang.isNumber (indexOrObject)) {
			proxy.remove (indexOrObject);
		} else {
			proxy.remove (indexOrObject.proxy);
		}
		return this;
	};
	
	/**	
	  Get the index of the given database object in this list
	  @param {DatabaseObject} - the database object
	  
	  @returns {number} the database object index position in this list
	*/
	this.indexOf = function (dbo) {
		if (!dbo) {
			throw 'indexOf of a null dbo argument';
		}
		return proxy.indexOf (dbo.proxy);
	};
	
	/**	
	  Check if thi list contains the database object given in arguent
	  @param {DatabaseObject} - the database object
	  
	  @returns {boolean} true if found
	*/
	this.contains = function (dbo) {
		if (!dbo) {
			throw 'contains of a null dbo argument';
		}
		return proxy.contains (dbo.proxy);
	};
	
	/**	
	  Check if this ist is empty

	  @returns {boolean} true if the list is empty
	*/
	this.isEmpty = function () {
		return proxy.isEmpty ();
	};
	
};
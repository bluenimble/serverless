/**
  Represents a Database instance<br/>
  <strong>Do not call constructor directly</strong>. 
  To create an database instance
  @see Api#database
  @class Database
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Database = function (api, proxy) {

	this.proxy = proxy;
	
	/**	
	  Get proprietary object by name
	  @param {string} - the object name
	  
	  @return {object} - the proprietary object
	*/
	this.proprietary = function (name) {
		return proxy.proprietary (name);
	};

	/**	
	  Start a database transaction
	*/
	this.trx = function () {
		proxy.trx ();
		return this;
	};

	/**	
	  Commit transaction
	*/
	this.commit = function () {
		proxy.commit ();
		return this;
	};

	/**	
	  Rollback transaction
	*/
	this.rollback = function () {
		proxy.rollback ();
		return this;
	};

	/**	
	  Create a database object for the given entity name
	  @param {string} - the entity/table name
	  @param {JsonObject} [data] - data to load on creation of this object
	  
	  @return {DatabaseObject} - the database object
	*/
	this.create = function (entity, data) {
		if (!entity) {
			throw "missing entity argument";
		}
		
		var dbo = new DatabaseObject (this, proxy.create (entity));
		
		if (data) {
			dbo.load (data);
		}
		
		return dbo;
	},

	/**	
	  Create a database object list for one2many and many2many reference objects
	  @return {DatabaseObjectList} - the database object list
	*/
	this.createList = function () {
		return new DatabaseObjectList (this, proxy.createList ());
	},

	/**	
	  Create or update a database object<br/>
	  If the database object id is present and it's found in the database, an update will be performed
	  @param {DatabaseObject} - the database object
	  
	  @return {DatabaseObject} - the database object
	*/
	this.put = function (dbo) {
		if (!dbo.proxy || dbo.clazz != 'DatabaseObject') {
			throw "invalid database object";
		}
		dbo.proxy.save ();
		return dbo;
	};
	
	/**	
	  Get a database object by id
	  @param {string} - the entity/table name
	  @param {Object} - the database object id
	  
	  @return {DatabaseObject} - the database object
	*/
	this.get = function (entity, id) {
		if (!entity || !id) {
			throw "both entity and id are required to get an object";
		}
		var o = proxy.get (entity, id);
		if (!o) {
			return null;
		}
		return new DatabaseObject (this, o);
	};
	
	/**	
	  Delete a database object
	  @param {string} - the entity/table name
	  @param {Object} - the database object id
	  
	  @return {integer} - positive value means that the object is found and it was deleted
	*/
	this.delete = function (entity, id) {
		if (!entity || !id) {
			throw "both entity and id are required to delete an object";
		}
		return proxy.delete (entity, id);
	};

	/**	
	  Delete records based on a query
	  @param {string} - the entity/table name
	  @param {JsonObject} - the query spec<br/>
	  @example
	  
	  db.deleteByQuery ({
	  	entity: 'YourEntityName',
	  	where: {
	  		prop1: '123',
	  		prop2: { op: 'gt', value: 35 }
	  	}
	  });
	  
	  @param {JsonObject} [bindings] - the query parameters
	  
	  @return {integer} - number of records deleted
	*/
	this.deleteByQuery = function (entity, query, bindings) {
		
		if (!entity) {
			throw "missing entity argument";
		}

		if (!query) {
			throw "missing query argument";
		}
		
		return proxy.delete (
			entity,
			new JC_JsonQuery (JC_ValueConverter.convert (query), bindings ? JC_ValueConverter.convert (bindings) : null)
		);
	};

	/**	
	  Update records based on a query
	  @param {string} - the entity/table name
	  @param {JsonObject} - the query spec<br/>
	  @param {JsonObject} - the data update object<br/>
	  @example
	  
	  db.update ({
	  	entity: 'YourEntityName',
	  	{
		  	where: {
		  		prop1: '123',
		  		prop2: { op: 'gt', value: 35 }
		  	}
	  	},
	  	{
	  		name: 'Alpha',
	  		'address.home': 'Somewhere'
	  	}
	  });
	  
	  @return {integer} - number of records updated
	*/
	this.update = function (entity, query, data) {
		
		if (!entity) {
			throw "missing entity argument";
		}

		if (!query) {
			throw "missing query argument";
		}
		
		if (!data) {
			throw "missing data argument";
		}
		
		return proxy.update (
			entity,
			new JC_JsonQuery (JC_ValueConverter.convert (query), null),
			JC_ValueConverter.convert (data)
		);
	};

	/**	
	  Count the number of records in this entity
	  @param {string} - the entity/table name

	  @return {integer} - number of records found
	*/
	this.count = function (entity) {
		if (!entity) {
			throw "missing entity argument";
		}
		return proxy.count (entity);
	};

	/**	
	  Find records based on a query
	  @param {string} - the entity/table name
	  @param {JsonObject} - the query spec<br/>
	  @example
	  
	  db.find (entity: 'YourEntityName', {
	  	where: {
	  		prop1: '123',
	  		prop2: { op: 'gt', value: 35 }
	  	}
	  }, function (dbo) {
	  	// do something useful with the DatabaseObject
	  });
	  
	  @param {JsonObject} - the callback function to execute for each record found. 
	  @param {JsonObject} [bindings] - the query parameters
	  
	*/
	this.find = function (entity, query, visitor, bindings) {
	
		if (!entity) {
			throw "missing entity argument";
		}

		if (!query) {
			throw "missing query argument";
		}
		
		var optimize = visitor.optimize;
		if (!optimize) {
			optimize = function () {
				return true;
			};
		}
		
		var onRecord = visitor.onRecord;
	
		if (!onRecord) {
			onRecord = visitor;
		}
		
		var db = this;
		
		var JVisitor = Java.extend (JC_Query_Visitor, {
			onRecord: function (jdbo) {
				return onRecord (new DatabaseObject (db, jdbo));
			},
			optimize: function () {
				return optimize ();
			}
		});
		
		proxy.find (
			entity,
			new JC_JsonQuery (JC_ValueConverter.convert (query), bindings ? JC_ValueConverter.convert (bindings) : null), 
			new JVisitor ()
		);
	};

	/**	
	  Find only 1 object matching your query
	  @param {string} - the entity/table name
	  @param {JsonObject} - the query spec<br/>
	  @example
	  
	  var dbo = db.findOne ('YourEntityName', {
	  	where: {
	  		prop1: '123',
	  		prop2: { op: 'gt', value: 35 }
	  	}
	  });
	  
	  @param {JsonObject} [bindings] - the query parameters
	  
	  @return {DatabaseObject} - the database object
	*/
	this.findOne = function (entity, query, bindings) {
		
		if (!entity) {
			throw "missing entity argument";
		}

		if (!query) {
			throw "missing query argument";
		}
		
		var dbo = proxy.findOne (
			entity,
			new JC_JsonQuery (JC_ValueConverter.convert (query), bindings ? JC_ValueConverter.convert (bindings) : null)
		);
		if (!dbo) {
			return;
		}
		return new DatabaseObject (
			this,
			dbo
		);
	};
	
	/**	
	  Pop records based on a query. It's a queue system, so records you pop are deleted
	  @param {string} - the entity/table name
	  @param {JsonObject} - the query spec<br/>
	  @example
	  
	  db.pop ('YourEntityName', {
	  	where: {
	  		prop1: '123',
	  		prop2: { op: 'gt', value: 35 }
	  	}
	  }, function (dbo) {
	  	// do something useful with the DatabaseObject
	  });
	  
	  @param {JsonObject} - the callback function to execute for each record found. 
	  @param {JsonObject} [bindings] - the query parameters
	  
	*/
	this.pop = function (entity, query, visitor, bindings) {
	
		if (!entity) {
			throw "missing entity argument";
		}

		if (!query) {
			throw "missing query argument";
		}
		
		var optimize = visitor.optimize;
		if (!optimize) {
			optimize = function () {
				return true;
			};
		}
		
		var onRecord = visitor.onRecord;
	
		if (!onRecord) {
			onRecord = visitor;
		}
		
		var db = this;
		
		var JVisitor = Java.extend (JC_Query_Visitor, {
			onRecord: function (jdbo) {
				return onRecord (new DatabaseObject (db, jdbo));
			},
			optimize: function () {
				return optimize ();
			}
		});
		
		proxy.pop (
			entity,
			new JC_JsonQuery (JC_ValueConverter.convert (query), bindings ? JC_ValueConverter.convert (bindings) : null), 
			new JVisitor ()
		);
	};

	/**	
	  Pop only 1 object matching your query
	  @param {string} - the entity/table name
	  @param {JsonObject} - the query spec<br/>
	  @example
	  
	  var dbo = db.popOne ('YourEntityName', {
	  	where: {
	  		prop1: '123',
	  		prop2: { op: 'gt', value: 35 }
	  	}
	  });
	  
	  @param {JsonObject} [bindings] - the query parameters
	  
	  @return {DatabaseObject} - the database object
	*/
	this.popOne = function (entity, query, bindings) {
		
		if (!entity) {
			throw "missing entity argument";
		}

		if (!query) {
			throw "missing query argument";
		}
		
		query [JC_Database_Fields.Entity] = entity;
		
		var dbo = proxy.popOne (
			entity,
			new JC_JsonQuery (JC_ValueConverter.convert (query), bindings ? JC_ValueConverter.convert (bindings) : null)
		);
		if (!dbo) {
			return;
		}
		return new DatabaseObject (
			this,
			dbo
		);
	};
	
	/**	
	  Delete all records of an entity<br /> 
	  <strong>Use with precaution!</strong>
	  @param {string} - the entity name
	*/
	this.clear = function (entity) {
		if (!entity) {
			throw "missing entity argument";
		}
		proxy.drop (entity);
	};
	
	/**	
	  Put multiple objects at once
	  @param {Array} - the array objects
	  @example
	  
	  var result = db.bulk ({
	  	Employees: [{
	  		name: 'John', age: 34	
	  	}, {
	  		name: 'Smith', age: 26	
	  	}],
	  	Pets: [{
	  		name: 'Rocky'
	  	}, {
	  		name: 'Rado'
	  	}]
	  });
	  
	  api.logger.info (result.totalCount);
	  api.logger.info (result.Employees.count);
	  
	  @return {Object} - an object containing all total counts
	  
	*/
	this.bulk = function (data) {
		if (!data) {
			throw "missing data argument";
		}
		if (!Lang.isObject (data)) {
			throw "missing data argument";
		}
		
		return proxy.bulk (JC_ValueConverter.convert (values));
	};

};

Database.prototype.Fields = {
	Entity: 	JC_Database_Fields.Entity,
	Id: 		JC_Database_Fields.Id,
	Timestamp:	JC_Database_Fields.Timestamp
};
/**
  Represents an Indexer instance<br/>
  <strong>Do not call constructor directly</strong>. 
  @see Api#indexer
  @class Indexer
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Indexer = function (proxy) {

	/**	
	  Create a type with a mapping data definition
	  @param {string} - entity name
	  @param {Object} - mapping definiton
	  @example
	  
	  api.indexer (request).create ('Orders', {
	    properties: {
	        description: {
	            type: 'text',
	            fields: {
	                keyword: {
	                    type: 'keyword'
	                }
	            }
	        },
	        price: {
	            type: 'double'
	        }
	    }
	  });
	  @return {Object} [result] - entity created
	*/
	this.create = function (entity, definiton) {
		return proxy.create (entity, JC_ValueConverter.convert (definition));
	};
	
	/**	
	  Put/index a document 
	  @param {string} - entity name
	  @param {Object} - document
	  @example
	  
	  api.indexer (request).put ('Orders', {
	  	id: '123456789',
	  	customer: 'john@bluenimble.com',
	  	price: 400,
	  	date: '2014/10/09T08:33:50'
	  });
	  @return {Object} [result] - document getting indexed
	*/
	this.put = function (entity, doc) {
		return proxy.put (typeof entity === 'undefined' ? null : entity, JC_ValueConverter.convert (doc));
	};
	
	/**	
	  Get a document by id
	  @param {string} - entity name
	  @param {string} - document id
	  @example
	  
	  api.indexer (request).get ('Order', '123456789');
	  
	  @return {Object} [result] - document data
	*/
	this.get = function (entity, id) {
		return proxy.get (typeof entity === 'undefined' ? null : entity, id);
	};
	
	/**	
	  Re-index fully or partially a document 
	  @param {string} - entity name
	  @param {Object} - document
	  @param {boolean} - true to partially update the document
	  @example
	  
	  api.indexer (request).update ('Orders', {
	  	id: '123456789',
	  	doc: {
	  		price: 500
	  	}
	  }, true );
	  
	  @return {Object} [result] - document getting re-indexed
	*/
	this.update = function (entity, doc, partial) {
		if (typeof partial === 'undefined' || partial === null) {
			partial = false;
		}
		return proxy.update (typeof entity === 'undefined' ? null : entity, JC_ValueConverter.convert (doc), partial);
	};
	
	/**	
	  Clear all documents in an entity 
	  @param {string} - entity name
	  @example
	  
	  api.indexer (request).clear ('Orders');
	  
	  @return {Object} [result] - document getting re-indexed
	*/
	this.clear = function (entity) {
		return proxy.clear (typeof entity === 'undefined' ? null : entity);
	};
	
	/**	
	  Delete a document by id
	  @param {string} - entity name
	  @param {string} - document id
	  @example
	  
	  api.indexer (request).delete ('Orders', '123456789');
	  
	  @return {Object} [result] - delete feedback
	*/
	this.delete = function (entity, id) {
		return proxy.delete (typeof entity === 'undefined' ? null : entity, id);
	};
	
	/**	
	  Search documents by query
	  @param {Object} - query
	  @param {Array} [entities] - array of entities names
	  @example
	  
	  api.indexer (request).search ({
	  	query: {
	  		term: { price : '400' }
	  	}
	  }, [
	  	'Orders'
	  ]);
	  
	  // search in all entities
	  api.indexer (request).search ({
	  	query: {
	  		term: { customer: 'john@bluenimble.com' }
	  	}
	  });
	  
	  @return {Object} [result] - search result
	*/
	this.search = function (dsl, entities) {
		if (!entities) {
			entities = null;
		} else {
			entities = Java.to (entities, "java.lang.String[]");
		}
		return proxy.search (JC_ValueConverter.convert (dsl), entities);
	};
	
	/**	
	  Count documents by query
	  @param {Object} - query
	  @param {Array} [entities] - array of entities names
	  @example
	  
	  api.indexer (request).count ({
	  	query: {
	  		term: { price : '400' }
	  	}
	  }, [
	  	'Orders'
	  ]);
	  
	  // search in all entities
	  api.indexer (request).count ({
	  	query: {
	  		term: { customer: 'john@bluenimble.com' }
	  	}
	  });
	  
	  @return {Number} [count] - number of documents matching the query
	*/
	this.count = function (dsl, entities) {
		if (!entities) {
			entities = null;
		} else {
			entities = Java.to (entities, "java.lang.String[]");
		}
		return proxy.count (JC_ValueConverter.convert (dsl), entities);
	};
	
	this.bulk = function (doc) {
		return proxy.bulk (JC_ValueConverter.convert (doc));
	};
	
	/**	
	  Find records based on a query
	  @param {string} - the entity/table name
	  @param {JsonObject} - the query spec<br/>
	  @example
	  
	  indexer.find ({
	  	where: {
	  		prop1: '123',
	  		prop2: { op: 'gt', value: 35 }
	  	}
	  }, function (record) {
	  	// do something useful with the JsonObject record
	  });
	  
	  @param {JsonObject} - the callback function to execute for each record found. 
	  @param {JsonObject} [bindings] - the query parameters
	  
	*/
	this.find = function (query, visitor, bindings) {
		if (!query) {
			throw "missing query argument";
		}
		
		var onRecord = visitor.onRecord;
		
		if (!onRecord) {
			onRecord = visitor;
		}
		
		var JVisitor = Java.extend (JC_Indexer_Visitor, {
			onRecord: function (record) {
				return onRecord (record);
			}
		});
		
		proxy.find (
			new JC_JsonQuery (JC_ValueConverter.convert (query), bindings ? JC_ValueConverter.convert (bindings) : null), 
			new JVisitor ()
		);
	};
	
	/**	
	  Find only 1 object matching your query
	  @param {string} - the entity/table name
	  @param {JsonObject} - the query spec<br/>
	  @example
	  
	  var record = indexer.findOne ('YourEntityName', {
	  	where: {
	  		prop1: '123',
	  		prop2: { op: 'gt', value: 35 }
	  	}
	  });
	  
	  @param {JsonObject} [bindings] - the query parameters
	  
	  @return {IndexRecord} - the index object
	*/
	this.findOne = function (query, bindings) {
		
		if (!query) {
			throw "missing query argument";
		}
		
		return proxy.findOne (
			new JC_JsonQuery (JC_ValueConverter.convert (query), bindings ? JC_ValueConverter.convert (bindings) : null)
		);
	};
};
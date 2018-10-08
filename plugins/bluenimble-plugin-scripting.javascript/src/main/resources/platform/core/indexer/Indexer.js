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
	  create a type with a mapping data definition
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
		return proxy.create (entity, JC_Converters.convert (definition));
	};
	
	/**	
	  put/index a document 
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
		return proxy.put (typeof entity === 'undefined' ? null : entity, JC_Converters.convert (doc));
	};
	
	/**	
	  get a document by id
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
	  re-index fully or partially a document 
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
		return proxy.update (typeof entity === 'undefined' ? null : entity, JC_Converters.convert (doc), partial);
	};
	
	/**	
	  clear all documents in an entity 
	  @param {string} - entity name
	  @example
	  
	  api.indexer (request).clear ('Orders');
	  
	  @return {Object} [result] - document getting re-indexed
	*/
	this.clear = function (entity) {
		return proxy.clear (typeof entity === 'undefined' ? null : entity);
	};
	
	/**	
	  delete a document by id
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
	  search documents by query
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
		return proxy.search (JC_Converters.convert (dsl), entities);
	};
	
};
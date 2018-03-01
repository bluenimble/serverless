/**
  Represents a Cache instance<br/>
  <strong>Do not call constructor directly</strong>. 
  @see Api#cache
  @class Cache
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Cache = function (proxy) {

	/**	
	  Creates a new cache bucket 
	  @param {string} - the bucket name
	*/
	this.create = function (bucket, ttl) {
		if (!ttl) {
			ttl = 0;
		}
		proxy.create (bucket, ttl);
	};
	
	/**	
	  Check if a cache bucket exists
	  @param {string} - the bucket name
	  
	  @returns {boolean} true if the bucket exists
	*/
	this.exists = function (bucket) {
		return proxy.exists (bucket);
	};
	
	/**	
	  Deletes a cache bucket with all entries stored in it
	  @param {string} - the bucket name
	*/
	this.drop = function (bucket) {
		proxy.drop (bucket);
	};
	
	/**	
	  Remove all the entries from a cache bucket
	  @param {string} - the bucket name
	*/
	this.clear = function (bucket) {
		proxy.clear (bucket);
	};
	
	/**	
	  List entries from a cache bucket
	  @param {string} - the bucket name
	  @param {integer} [start=0] - start form position
	  @param {integer} [page=25] - page size

	  @return {JsonObject} - list of entries
	*/
	this.list = function (bucket, start, page) {
		return proxy.get (bucket, start, page);
	};
	
	/**	
	  Add or update an entry into a cache bucket
	  @param {string} - the bucket name
	  @param {string} - entry key
	  @param {object} - entry value
	  @param {integer} [ttl=0] - entry time-to-live in seconds. 0, means it will never leave the bucket

	  @return {JsonObject} - list of entries
	*/
	this.put = function (bucket, key, value, ttl) {
		if (!ttl) {
			ttl = 0;
		}
		return proxy.put (bucket, key, value, ttl);
	};
	
	/**	
	  Get an entry from a cache bucket
	  @param {string} - the bucket name
	  @param {string} - entry key
	  @param {boolean} [remove=false] - remove this entry on get

	  @return {object} - entry value
	*/
	this.get = function (bucket, key, remove) {
		if (typeof remove === 'undefined' || remove === null) {
			remove = false;
		}
		return proxy.get (bucket, key, remove);
	};
	
	/**	
	  Remove an entry from a cache bucket
	  @param {string} - the bucket name
	  @param {string} - entry key
	*/
	this.delete = function (bucket, key) {
		proxy.delete (bucket, key);
	};
	
};
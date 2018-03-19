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
	  Add or update an entry into the cache
	  @param {string} - entry key
	  @param {object} - entry value
	  @param {integer} [ttl=0] - entry time-to-live in seconds. 0, means it will never leave the cache

	  @return {JsonObject} - list of entries
	*/
	this.put = function (key, value, ttl) {
		if (!ttl) {
			ttl = 0;
		}
		return proxy.put (key, value, ttl);
	};
	
	/**	
	  Get an entry from the cache
	  @param {string} - entry key
	  @param {boolean} [remove=false] - remove this entry on get

	  @return {object} - entry value
	*/
	this.get = function (key, remove) {
		if (typeof remove === 'undefined' || remove === null) {
			remove = false;
		}
		return proxy.get (key, remove);
	};
	
	/**	
	  Remove an entry
	  @param {string} - entry key
	*/
	this.delete = function (key) {
		proxy.delete (key);
	};
	
	/**	
	  Increment or Decrement an entry by the value 'increment', set a default value of 'dValue' if the entry doesn't exist
	  @param {string} - entry key
	  @param {integer} - positive or negative value of the increment 
	  @param {integer} - default value if key does not exist
	  @param {integer} [ttl=0] - entry time-to-live in seconds. 0, means it will never leave the cache
	  @param {boolean} [async=false] - run in async mode
	*/
	this.increment = function (key, increment, dValue, ttl, async) {
		proxy.increment (key, increment, dValue, ttl, async);
	};
	
};
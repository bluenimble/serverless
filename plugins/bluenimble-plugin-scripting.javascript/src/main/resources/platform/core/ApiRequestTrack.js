/**
  Represents the logger of an api running in your space 
  <strong>Do not call constructor directly</strong>
  Bluenimble container creates it internally and makes it available for your services to use 	
  @class ApiRequestTrack
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiRequestTrack = function (proxy) {

	/**	
	  Put application data into the request track
	  @param {string} key - key 
	  @param {Object} value - value
	  
	  @returns {ApiRequestTrack} the track itself
	*/
	this.put = function (key, value) {
		if (!key || !value) {
			return this;
		}
		proxy.put (key, value);
		return this;
	};
	
	/**	
	  Tag a request
	  @param {string} name - name
	  @param {Object} reason - reason for the tag
	  
	  @returns {ApiRequestTrack} the track itself
	*/
	this.tag = function (name, reason) {
		if (!name) {
			return this;
		}
		proxy.tag (name, reason);
		return this;
	};
	
	/**	
	  Discard this track
	*/
	this.discard = function () {
		proxy.discard ();
	};
	
};
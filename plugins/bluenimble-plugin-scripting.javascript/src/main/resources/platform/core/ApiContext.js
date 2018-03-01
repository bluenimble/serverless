/**
  Represents a context of the running code.<br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use 	
  @class ApiContext
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiContext = function (proxy) {
	this.proxy = proxy;	
};
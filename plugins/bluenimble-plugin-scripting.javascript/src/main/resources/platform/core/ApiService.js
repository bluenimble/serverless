/**
  Represents a service running in an api under your space <br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use 	
  @class ApiService
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiService = function (proxy) {

	this.proxy 			= proxy;

	/**	
	  The name of this service
	  @type {string}
	  @readonly
	*/
	this.name 			= proxy.getName 		();
	/**	
	  The description of this service
	  @type {string}
	  @readonly
	*/
	this.description 	= proxy.getDescription 	();
	/**	
	  The endpoint of this service
	  @type {string}
	  @readonly
	*/
	this.endpoint 		= proxy.getEndpoint 	();
	
	/**	
	  The security block in the service json spec - if any -
	  @type {JsonObject}
	*/
	this.security 		= proxy.getSecurity 	();
	/**	
	  The runtime block in the service json spec - if any -
	  @type {JsonObject}
	*/
	this.runtime		= proxy.getRuntime 		();
	/**	
	  The features block in the service json spec - if any -
	  @type {JsonObject}
	*/
	this.features		= proxy.getFeatures 	();
	/**	
	  The media block in the service json spec - if any -
	  @type {JsonObject}
	*/
	this.media			= proxy.getMedia 		();
	/**	
	  The custom block in the service json spec - if any -
	  @type {JsonObject}
	*/
	this.custom			= proxy.getCustom 		();
	
	/**	
	  The error message in case of a failure during deployment of this service
	  @type {string}
	  @readonly
	*/
	this.failure		= proxy.getFailure ();
	
	/**	
	  The runtime status of this service. Running, Stopped, Paused or Failed
	  @type {string}
	*/
	this.status = function () {
		return proxy.status ().name ();
	};
	/**	
	  Pause this service.<br/>
	  All upcoming requests for this service will be rejected if the service is paused
	*/
	this.pause = function () {
		proxy.pause ();
	};
	/**	
	  Resume this service
	*/
	this.resume = function () {
		proxy.resume ();
	};
	/**	
	  Get a json representation of this service
	  
	  @returns {JsonObject} service as a json object
	*/
	this.toJson = function () {
		return proxy.toJson ();
	};
	
};
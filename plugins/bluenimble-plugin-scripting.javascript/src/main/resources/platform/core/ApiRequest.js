/**
  Represents a context of the running code<br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use 	
  @class ApiRequest
  @classdesc 
*/

/**
  @constructor
  @access private
  @augments ApiContext
*/
var ApiRequest = function (proxy) {

	this.proxy 		= proxy;
	
	/**	
	  The service resolved by the container to execute this request 
	  @type {ApiService}
	  @readonly
	*/
	this.service 	= new ApiService (proxy.getService ());
	
	/**	
	  The request track.  
	  @type {ApiRequestTrack}
	  @readonly
	*/
	this.track 		= new ApiRequestTrack (proxy.track ());

	/**	
	  The id of this request 
	  @type {string}
	  @readonly
	*/
	this.id 		= proxy.getId ();
	/**	
	  The time when this request was created/received by the container
	  @type {Date}
	  @readonly
	*/
	this.timestamp 	= proxy.getTimestamp ();
	
	/**	
	  The channel from where this request is coming from http, coap, mqtt or container
	  @type {string}
	  @readonly
	*/
	this.channel 	= proxy.getChannel ();
	this.scheme 	= proxy.getScheme ();
	/**	
	  The server endpoint of this request
	  @type {string}
	  @readonly
	*/
	this.endpoint 	= proxy.getEndpoint ();
	/**	
	  The verb of this request GET, POST, PUT, DELETE
	  @type {string}
	  @readonly
	*/
	this.verb 		= proxy.getVerb ().name ();
	
	/**	
	  The space resolved for this request
	  @type {string}
	  @readonly
	*/
	this.space 		= proxy.getSpace ();
	/**	
	  The api namespace resolved to run this request
	  @type {string}
	  @readonly
	*/
	this.api 		= proxy.getApi ();
	
	/**	
	  The array of resources resolved from the path
	  @type {Array}
	  @readonly
	*/
	this.resource 	= proxy.getResource ();
	/**	
	  The path of this request
	  @type {Array}
	  @readonly
	*/
	this.path 		= proxy.getPath ();
	/**	
	  The language of the the calling device of application
	  @type {string}
	  @readonly
	*/
	this.lang 		= proxy.getLang ();
	/**	
	  The device making this request
	  @type {JsonObject}
	  @property {string} origin - the ip or hostname of the application or device
      @property {string} lang - the language of the application or user
      @property {string} agent - the software used to make the request
      @property {string} os - the operating system of the calling application or device
	  @readonly
	*/
	this.device 	= proxy.getDevice ();
	
	/**	
	  Request parameters
	  @type {Array}
	  @readonly
	*/
	this.params 	= [];
	/**	
	  Request headers http headers, coap options or mqtt attributes
	  @type {Array}
	  @readonly
	*/
	this.headers 	= [];
	/**	
	  Request streams such as file uploads
	  @type {Array}
	  @readonly
	*/
	this.streams 	= [];
	
	var pKeys = proxy.keys (JC_ApiRequest_Scope.Parameter);
	if (pKeys) {
		while (pKeys.hasNext ()) {
			this.params.push (pKeys.next ());
		}
	}
	var hKeys = proxy.keys (JC_ApiRequest_Scope.Header);
	if (hKeys) {
		while (hKeys.hasNext ()) {
			this.headers.push (hKeys.next ());
		}
	}
	var sKeys = proxy.keys (JC_ApiRequest_Scope.Stream);
	if (sKeys) {
		while (sKeys.hasNext ()) {
			this.streams.push (sKeys.next ());
		}
	}
	
	/**	
	  The path of this request
	  @type {Array}
	  @readonly
	*/
	this.parent 		= proxy.getParent () != null ? new ApiRequest (proxy.getParent ()) : null;

	/**	
	  Set a parameter, header 
	  @param {string} - the name of the parameter or header
	  @param {Object} - the value of the parameter or header
	  @param {ApiRequest.Scope} [scope=ApiRequest.Parameter] - the scope of the key ApiRequest.Parameter or ApiRequest.Header
	*/
	this.set = function (key, value, scope) {
		if (!scope) {
			scope = this.Scope.Parameter;
		}
		proxy.set (key, value, JC_ApiRequest_Scope.valueOf (scope));
	};
	
	/**	
	  Get a value from a specific scope  
	  @param {string} - the name of the parameter, header or stream
	  @param {ApiRequest.Scope} [scope=ApiRequest.Scope.Parameter] - the scope of the key ApiRequest.Scope.Parameter, ApiRequest.Scope.Header or ApiRequest.Scope.Stream
	  
	  @returns {Object} the value of the parameter, header or stream 
	*/
	this.get = function (key, scope) {
		if (!scope) {
			scope = this.Scope.Parameter;
		}
		
		var v = proxy.get (key, JC_ApiRequest_Scope.valueOf (scope));
		if (typeof v == undefined) {
			return;
		}
		
		if (scope == this.Scope.Parameter && key == JC_ContainerApiRequest.Consumer) {
			return new ApiConsumer (v);
		}
		
		if (scope == this.Scope.Stream) {
			return new ApiStreamSource (v);
		} 
		return v;
	};
	
	/**	
	  Get a json representation of this request  
	  
	  @returns {JsonObject} request as a json object
	*/
	this.toJson = function () {
		return proxy.toJson ();
	};
	
	/**	
	  Get parameters starting with a specific prefix
	  @param {string} - prefix
	  @param {ApiRequest.Scope} [scope=ApiRequest.Parameter] - the scope. ApiRequest.Parameter, ApiRequest.Header or ApiRequest.Stream
	  @returns {JsonObject} an object with all parameters and corresponding values 
	*/
	this.withPrefix = function (prefix, scope) {
		if (!scope) {
			scope = JC_ApiRequest_Scope.Parameter;
		} else {
			scope = JC_ApiRequest_Scope.valueOf (scope);
		}
		var pKeys = proxy.keys (scope);
		if (!pKeys) {
			return;
		}
		var selected = {};
		while (pKeys.hasNext ()) {
			var key = pKeys.next ();
			if (key.indexOf (prefix) != 0) {
				continue;
			}
			selected [key] = proxy.get (key, scope);
		}
		return selected;
	};
	
};
ApiRequest.prototype.Scope = {
	Header: 	'Header',
	Parameter: 	'Parameter',
	Stream: 	'Stream'
};
ApiRequest.prototype.Consumer 		= JC_ApiRequest.Consumer;
ApiRequest.prototype.Caller 		= JC_ApiRequest.Caller;

ApiRequest.prototype.Output 		= JC_ApiRequest.Output;

ApiRequest.prototype.Payload 		= JC_ApiRequest.Payload;
ApiRequest.prototype.Transport 		= JC_ApiRequest.Transport;

ApiRequest.prototype.MediaType 		= JC_ApiRequest.MediaType;
ApiRequest.prototype.MediaSelector 	= JC_ApiRequest.MediaSelector;

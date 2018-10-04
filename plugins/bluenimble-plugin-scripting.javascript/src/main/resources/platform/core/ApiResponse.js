/**
  Represents an ApiResponse<br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use 	
  @class ApiResponse
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiResponse = function (proxy) {

	this.proxy 	= proxy;
	
	if (proxy.getStatus ()) {
		/**	
		  The status of this response
		  @type {integer}
		  @readonly
		*/
		this.status = proxy.getStatus ().getCode ();
	}

	/**	
	  Set a header value
	  @param {string} - the name of the header
	  @param {Object} - the value of the header
	*/
	this.set = function (key, value) {
		proxy.set (key, value);
	};
	
	/**	
	  Set the status of this response
	  @param {integer} - the status code
	*/
	this.setStatus = function (status) {
		if (!status) {
			return;
		}
		if (typeof status === 'string') {
			try {
				status = parseInt (status);
			} catch (e) {
				status = 0;
			}
		}
		if (status <= 0) {
			return;
		}
		var jstatus = JC_HttpStatues.Map.get (status);
		if (jstatus) {
			this.status = status;
			proxy.setStatus (jstatus);
		}
	};
	
	/**	
	  Attach an error to the response
	  @param {integer} - the status code
	  @param {Object} - the error message
	*/
	this.error = function (status, message) {
		if (!status) {
			return;
		}
		if (typeof status === 'string') {
			try {
				status = parseInt (status);
			} catch (e) {
				status = 0;
			}
		}
		if (status <= 0) {
			return;
		}
		proxy.error (JC_HttpStatues.Map.get (status), message);
	};
	
};

ApiResponse.prototype.BAD_REQUEST						= JC_ApiResponse.BAD_REQUEST;
ApiResponse.prototype.UNAUTHORIZED						= JC_ApiResponse.UNAUTHORIZED;
ApiResponse.prototype.PAYMENT_REQUIRED					= JC_ApiResponse.PAYMENT_REQUIRED;
ApiResponse.prototype.FORBIDDEN							= JC_ApiResponse.FORBIDDEN;
ApiResponse.prototype.NOT_FOUND							= JC_ApiResponse.NOT_FOUND;
ApiResponse.prototype.METHOD_NOT_ALLOWED				= JC_ApiResponse.METHOD_NOT_ALLOWED;
ApiResponse.prototype.NOT_ACCEPTABLE					= JC_ApiResponse.NOT_ACCEPTABLE;
ApiResponse.prototype.PROXY_AUTHENTICATION_REQUIRED		= JC_ApiResponse.PROXY_AUTHENTICATION_REQUIRED;
ApiResponse.prototype.REQUEST_TIMEOUT					= JC_ApiResponse.REQUEST_TIMEOUT;
ApiResponse.prototype.CONFLICT							= JC_ApiResponse.CONFLICT;
ApiResponse.prototype.GONE								= JC_ApiResponse.GONE;
ApiResponse.prototype.LENGTH_REQUIRED					= JC_ApiResponse.LENGTH_REQUIRED;
ApiResponse.prototype.PRECONDITION_FAILED				= JC_ApiResponse.PRECONDITION_FAILED;
ApiResponse.prototype.REQUEST_TOO_LONG					= JC_ApiResponse.REQUEST_TOO_LONG;
ApiResponse.prototype.REQUEST_URI_TOO_LONG				= JC_ApiResponse.REQUEST_URI_TOO_LONG;
ApiResponse.prototype.UNSUPPORTED_MEDIA_TYPE			= JC_ApiResponse.UNSUPPORTED_MEDIA_TYPE;
ApiResponse.prototype.REQUESTED_RANGE_NOT_SATISFIABLE	= JC_ApiResponse.REQUESTED_RANGE_NOT_SATISFIABLE;
ApiResponse.prototype.EXPECTATION_FAILED				= JC_ApiResponse.EXPECTATION_FAILED;
ApiResponse.prototype.INSUFFICIENT_SPACE_ON_RESOURCE	= JC_ApiResponse.INSUFFICIENT_SPACE_ON_RESOURCE;
ApiResponse.prototype.METHOD_FAILURE					= JC_ApiResponse.METHOD_FAILURE;
ApiResponse.prototype.UNPROCESSABLE_ENTITY				= JC_ApiResponse.UNPROCESSABLE_ENTITY;
ApiResponse.prototype.LOCKED							= JC_ApiResponse.LOCKED;
ApiResponse.prototype.FAILED_DEPENDENCY					= JC_ApiResponse.FAILED_DEPENDENCY;

ApiResponse.prototype.INTERNAL_SERVER_ERROR				= JC_ApiResponse.INTERNAL_SERVER_ERROR;
ApiResponse.prototype.NOT_IMPLEMENTED					= JC_ApiResponse.NOT_IMPLEMENTED;
ApiResponse.prototype.BAD_GATEWAY						= JC_ApiResponse.BAD_GATEWAY;
ApiResponse.prototype.SERVICE_UNAVAILABLE				= JC_ApiResponse.SERVICE_UNAVAILABLE;
ApiResponse.prototype.GATEWAY_TIMEOUT					= JC_ApiResponse.GATEWAY_TIMEOUT;
ApiResponse.prototype.HTTP_VERSION_NOT_SUPPORTED		= JC_ApiResponse.HTTP_VERSION_NOT_SUPPORTED;
ApiResponse.prototype.INSUFFICIENT_STORAGE				= JC_ApiResponse.INSUFFICIENT_STORAGE;


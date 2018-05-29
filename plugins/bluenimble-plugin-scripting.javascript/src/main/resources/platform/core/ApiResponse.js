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

ApiRequest.prototype.BAD_REQUEST						= JC_ApiResponse.BAD_REQUEST;
ApiRequest.prototype.UNAUTHORIZED						= JC_ApiResponse.UNAUTHORIZED;
ApiRequest.prototype.PAYMENT_REQUIRED					= JC_ApiResponse.PAYMENT_REQUIRED;
ApiRequest.prototype.FORBIDDEN							= JC_ApiResponse.FORBIDDEN;
ApiRequest.prototype.NOT_FOUND							= JC_ApiResponse.NOT_FOUND;
ApiRequest.prototype.METHOD_NOT_ALLOWED					= JC_ApiResponse.METHOD_NOT_ALLOWED;
ApiRequest.prototype.NOT_ACCEPTABLE						= JC_ApiResponse.NOT_ACCEPTABLE;
ApiRequest.prototype.PROXY_AUTHENTICATION_REQUIRED		= JC_ApiResponse.PROXY_AUTHENTICATION_REQUIRED;
ApiRequest.prototype.REQUEST_TIMEOUT					= JC_ApiResponse.REQUEST_TIMEOUT;
ApiRequest.prototype.CONFLICT							= JC_ApiResponse.CONFLICT;
ApiRequest.prototype.GONE								= JC_ApiResponse.GONE;
ApiRequest.prototype.LENGTH_REQUIRED					= JC_ApiResponse.LENGTH_REQUIRED;
ApiRequest.prototype.PRECONDITION_FAILED				= JC_ApiResponse.PRECONDITION_FAILED;
ApiRequest.prototype.REQUEST_TOO_LONG					= JC_ApiResponse.REQUEST_TOO_LONG;
ApiRequest.prototype.REQUEST_URI_TOO_LONG				= JC_ApiResponse.REQUEST_URI_TOO_LONG;
ApiRequest.prototype.UNSUPPORTED_MEDIA_TYPE				= JC_ApiResponse.UNSUPPORTED_MEDIA_TYPE;
ApiRequest.prototype.REQUESTED_RANGE_NOT_SATISFIABLE	= JC_ApiResponse.REQUESTED_RANGE_NOT_SATISFIABLE;
ApiRequest.prototype.EXPECTATION_FAILED					= JC_ApiResponse.EXPECTATION_FAILED;
ApiRequest.prototype.INSUFFICIENT_SPACE_ON_RESOURCE		= JC_ApiResponse.INSUFFICIENT_SPACE_ON_RESOURCE;
ApiRequest.prototype.METHOD_FAILURE						= JC_ApiResponse.METHOD_FAILURE;
ApiRequest.prototype.UNPROCESSABLE_ENTITY				= JC_ApiResponse.UNPROCESSABLE_ENTITY;
ApiRequest.prototype.LOCKED								= JC_ApiResponse.LOCKED;
ApiRequest.prototype.FAILED_DEPENDENCY					= JC_ApiResponse.FAILED_DEPENDENCY;

ApiRequest.prototype.INTERNAL_SERVER_ERROR				= JC_ApiResponse.INTERNAL_SERVER_ERROR;
ApiRequest.prototype.NOT_IMPLEMENTED					= JC_ApiResponse.NOT_IMPLEMENTED;
ApiRequest.prototype.BAD_GATEWAY						= JC_ApiResponse.BAD_GATEWAY;
ApiRequest.prototype.SERVICE_UNAVAILABLE				= JC_ApiResponse.SERVICE_UNAVAILABLE;
ApiRequest.prototype.GATEWAY_TIMEOUT					= JC_ApiResponse.GATEWAY_TIMEOUT;
ApiRequest.prototype.HTTP_VERSION_NOT_SUPPORTED			= JC_ApiResponse.HTTP_VERSION_NOT_SUPPORTED;
ApiRequest.prototype.INSUFFICIENT_STORAGE				= JC_ApiResponse.INSUFFICIENT_STORAGE;


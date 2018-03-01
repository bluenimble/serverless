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
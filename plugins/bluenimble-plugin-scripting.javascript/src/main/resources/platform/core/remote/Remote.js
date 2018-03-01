function toJavaAttachments (attachments) {
	if (!attachments || !attachments.length || attachments.length <= 0) {
		return null;
	}
	var jAttachments = [];
	for (var i = 0; i < attachments.length; i++) {
		jAttachments.push (attachments [i].proxy);
	}
	return jAttachments;
}

var NoCallback = {
	onSuccess: function (code, payload) {},
	onError: function (code, message) {}
};

/**
  Represents a Remote instance<br/>
  <strong>Do not call constructor directly</strong>. 
  @see Api#remote
  @class Remote
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Remote = function (proxy) {

	/**	
	  Send a get request 
	  @param {Object} - request spec
	  @param {Object} - callback
	  @example
	  
	  api.remote (request).get ({
	  	endpoint: 'https://myserver.com',
	  	data: {
	  		email: 'john@bluenimble.com'
	  	}
	  }, {
	  	onSuccess: function (code, data) {
	  		api.logger.info (code + ': ' + data);
	  	},
	  	onError: function (code, message) {
	  		api.logger.error (code + ': ' + message);
	  	}
	  });
	*/
	this.get = function (spec, callback) {
		return proxy.get (
			JC_Converters.convert (spec),
			this._callback (callback)
		);
	};
	
	/**	
	  Send a post request 
	  @param {Object} - request spec
	  @param {Object} - callback
	  @param {Array} - attachments
	  @example
	  
	  api.remote (request).post ({
	  	endpoint: 'https://myserver.com',
	  	data: {
	  		name: 'John', 
	  		age: 30, 
	  		address: '100 Rockefeller Dr, Sunnyvale CA'
	  	},
	  	headers: {
	  		'Content-Type': 'application/json'
	  	}
	  }, {
	  	onSuccess: function (code, data) {
	  		api.logger.info (code + ': ' + data);
	  	},
	  	onError: function (code, message) {
	  		api.logger.error (code + ': ' + message);
	  	}
	  });
	*/
	this.post = function (spec, callback, attachments) {
		return proxy.post (
			JC_Converters.convert (spec),
			this._callback (callback),
			toJavaAttachments (attachments)
		);
	};
	
	/**	
	  Send a put request 
	  @param {Object} - request spec
	  @param {Object} - callback
	  @param {Array} - attachments
	  @example
	  
	  api.remote (request).put ({
	  	endpoint: 'https://myserver.com/123456789',
	  	data: {
	  		address: '200 Sheraton Dr, Sunnyvale CA'
	  	},
	  	headers: {
	  		'Content-Type': 'application/json'
	  	}
	  }, {
	  	onSuccess: function (code, data) {
	  		api.logger.info (code + ': ' + data);
	  	},
	  	onError: function (code, message) {
	  		api.logger.error (code + ': ' + message);
	  	}
	  });
	*/
	this.put = function (spec, callback, attachments) {
		return proxy.put (
			JC_Converters.convert (spec),
			this._callback (callback),
			toJavaAttachments (attachments)
		);
	};
	
	/**	
	  Send a delete request 
	  @param {Object} - request spec
	  @param {Object} - callback
	  @param {Array} - attachments
	  @example
	  
	  api.remote (request).delete ({
	  	endpoint: 'https://myserver.com/123456789'
	  }, {
	  	onSuccess: function (code, data) {
	  		api.logger.info (code + ': ' + data);
	  	},
	  	onError: function (code, message) {
	  		api.logger.error (code + ': ' + message);
	  	}
	  });
	*/
	this.delete = function (spec, callback) {
		return proxy.delete (
			JC_Converters.convert (spec),
			this._callback (callback)
		);
	};
	
	/**	
	  Send a head request 
	  @param {Object} - request spec
	  @param {Object} - callback
	  @param {Array} - attachments
	  @example
	  
	  api.remote (request).head ({
	  	endpoint: 'https://myserver.com/123456789'
	  }, {
	  	onSuccess: function (code, data) {
	  		api.logger.info (code + ': ' + data);
	  	},
	  	onError: function (code, message) {
	  		api.logger.error (code + ': ' + message);
	  	}
	  });
	*/
	this.head = function (spec, callback) {
		return proxy.head (
			JC_Converters.convert (spec),
			this._callback (callback)
		);
	};
	
	/**	
	  Send a patch request 
	  @param {Object} - request spec
	  @param {Object} - callback
	  @param {Array} - attachments
	  @example
	  
	  api.remote (request).patch ({
	  	endpoint: 'https://myserver.com/123456789'
	  }, {
	  	onSuccess: function (code, data) {
	  		api.logger.info (code + ': ' + data);
	  	},
	  	onError: function (code, message) {
	  		api.logger.error (code + ': ' + message);
	  	}
	  });
	*/
	this.patch = function (spec, callback) {
		return proxy.patch (
			JC_Converters.convert (spec),
			this._callback (callback)
		);
	};
	
	this._callback = function (callback) {
		if (!callback) {
			callback = NoCallback;
		}
	
		var JC_Callback = Java.extend (JC_Remote_Callback, {
			onSuccess: function (code, payload) {
				callback.onSuccess (code, payload);
			},
			onError: function (code, message) {
				callback.onError (code, message);
			}
		});
		
		return new JC_Callback ();
	}
	
};
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
	onStatus: function (status, chunked, headers) {},
	onData: function (status, chunk) {},
	onDone: function (status, data) {},
	onError: function (status, message) {}
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
	  	onStatus: function (status, chunked, headers) {
	  		api.logger.info (status + ': chunked: ' + chunked + ' > ' + headers);
	  	},
	  	onDone: function (status, data) {
	  		api.logger.info (status + ': data);
	  	},
	  	onError: function (status, message) {
	  		api.logger.error (status + ': ' + message);
	  	}
	  });
	*/
	this.get = function (spec, callback) {
		proxy.get (
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
	  	onStatus: function (status, chunked, headers) {
	  		api.logger.info (status + ': chunked: ' + chunked + ' > ' + headers);
	  	},
	  	onDone: function (status, data) {
	  		api.logger.info (status + ': data);
	  	},
	  	onError: function (status, message) {
	  		api.logger.error (status + ': ' + message);
	  	}
	  });
	*/
	this.post = function (spec, callback, attachments) {
		proxy.post (
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
	  	onStatus: function (status, chunked, headers) {
	  		api.logger.info (status + ': chunked: ' + chunked + ' > ' + headers);
	  	},
	  	onDone: function (status, data) {
	  		api.logger.info (status + ': data);
	  	},
	  	onError: function (status, message) {
	  		api.logger.error (status + ': ' + message);
	  	}
	  });
	*/
	this.put = function (spec, callback, attachments) {
		proxy.put (
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
	  	onStatus: function (status, chunked, headers) {
	  		api.logger.info (status + ': chunked: ' + chunked + ' > ' + headers);
	  	},
	  	onDone: function (status, data) {
	  		api.logger.info (status + ': data);
	  	},
	  	onError: function (status, message) {
	  		api.logger.error (status + ': ' + message);
	  	}
	  });
	*/
	this.delete = function (spec, callback) {
		proxy.delete (
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
	  	onStatus: function (status, chunked, headers) {
	  		api.logger.info (status + ': chunked: ' + chunked + ' > ' + headers);
	  	},
	  	onDone: function (status, data) {
	  		api.logger.info (status + ': data);
	  	},
	  	onError: function (status, message) {
	  		api.logger.error (status + ': ' + message);
	  	}
	  });
	*/
	this.head = function (spec, callback) {
		proxy.head (
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
	  	onStatus: function (status, chunked, headers) {
	  		api.logger.info (status + ': chunked: ' + chunked + ' > ' + headers);
	  	},
	  	onDone: function (status, data) {
	  		api.logger.info (status + ': data);
	  	},
	  	onError: function (status, message) {
	  		api.logger.error (status + ': ' + message);
	  	}
	  });
	*/
	this.patch = function (spec, callback) {
		proxy.patch (
			JC_Converters.convert (spec),
			this._callback (callback)
		);
	};
	
	this._callback = function (callback) {
		if (!callback) {
			callback = NoCallback;
		}
	
		var JC_Callback = Java.extend (JC_Remote_Callback, {
			
			onStatus: function (status, chunked, headers) {
				if (!callback.onStatus) {
					return;
				}
				callback.onStatus (status, chunked, headers);
			},
			onData: function (status, chunk) {
				if (!callback.onData) {
					return;
				}
				callback.onData (status, chunk);
			},
			onDone: function (status, data) {
				if (!callback.onDone) {
					return;
				}
				callback.onDone (status, data);
			},
			onError: function (status, message) {
				if (!callback.onError) {
					return;
				}
				callback.onError (status, message);
			}
		});
		
		return new JC_Callback ();
	}
	
};
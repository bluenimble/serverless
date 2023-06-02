/**
  Represents an api running in your space <br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use 	
  @class Api
  @classdesc 
*/

/**
  @constructor
  @access private
*/
function Api (proxy) {

	this.proxy = proxy;

	/**	
	  The namespace of this api 
	  @type {string}
	  @readonly
	*/
	this.namespace 		= proxy.getNamespace ();
	/**	
	  The name of this api 
	  @type {string}
	  @readonly
	*/
	this.name 			= proxy.getName ();
	/**	
	  The description of this api 
	  @type {string}
	  @readonly
	*/
	this.description 	= proxy.getDescription ();
	/**	
	  The error message in case of a failure during deployment of this api 
	  @type {string}
	  @readonly
	*/
	this.failure		= proxy.getFailure ();
	
	/**	
	  The runtime block of this api - if any - 
	  @type {JsonObject}
	*/
	this.runtime		= proxy.getRuntime ();
	/**	
	  The spi block of this api - if any -
	  @type {JsonObject}
	*/
	this.spiDef			= proxy.getSpiDef 	();
	/**	
	  The features block of this api - if any -
	  @type {JsonObject}
	*/
	this.features		= proxy.getFeatures ();
	/**	
	  The security block of this api - if any -
	  @type {JsonObject}
	*/
	this.security		= proxy.getSecurity ();
	/**	
	  The logger of this api
	  @type {Tracer}
	*/
	this.tracer				= new Tracer (proxy.tracer ());
	
	/**	
	  Get the resources manager of this api
	  @type {ApiResourcesManager}
	*/
	this.resourcesManager	= new ApiResourcesManager (proxy.getResourcesManager ());

	/**	
	  Get the services manager of this api
	  @type {ApiServicesManager}
	*/
	this.servicesManager	= new ApiServicesManager (proxy.getServicesManager ());

	/**	
	  The runtime status of this api. Running, Stopped, Paused or Failed
	  @type {string}
	*/
	this.status = function () {
		return proxy.status ().name ();
	};
	/**	
	  Create an i18n message or text for a specific language and key.
	  Keys used should be defined under resources/messages folder of the api
	  @param {string} lang - target language us, es, fr, ...
	  @param {string} key - the key of text or message 
	  @param {varray} values - extra values to format the message given by the key
	  
	  @returns {string} message
	*/
	this.message = function () {
		var args 	= Array.prototype.slice.call (arguments);
		var lang 	= args [0];
		var key 	= args [1];
		
		return proxy.message (lang, key, Java.to (args.slice (2), "java.lang.Object[]"));
	};
	/**	
	  Get all messages in this api for a specific language
	  @param {string} lang - target language us, es, fr, ...
	  
	  @returns {JsonObject} messages object
	*/
	this.i18n = function (lang) {
		return proxy.i18n (lang);
	};
	/**	
	  Describe this api.
	  Calling describe (true), will describe all services of the api and their corresponding runtime status
	  @param {boolean} full - fully describe
	  
	  @returns {JsonObject} a Json Object describing the api
	*/
	this.describe = function (full) {
		if (!full) {
			full = true;
		}
		if (!(typeof full === 'boolean')) {
			full = JC_Lang.TrueValues.contains (a);				
		}
		return proxy.describe (full);
	};
	
	/**	
	  Create a new Database feature object.
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the database handle will be associated with
	  @param {string} [feature=default] the name of the database feature
	  
	  @returns {Database} an instance of a database object
	*/
	this.database = function (context, feature) {
		if (!context) {
			throw "missing argument context";
		}
		feature = this._feature ('database', feature);
		return new Database (proxy, JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Database, context.proxy, feature));
	};
	
	/**	
	  Create a new Storage feature object.
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the storage handle will be associated with
	  @param {string} [feature=default] the name of the database feature
	  @returns {Storage} an instance of a database object
	*/
	this.storage = function (context, feature) {
		if (!context) {
			throw "missing argument context";
		}
		feature = this._feature ('storage', feature);
		return new Storage (JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Storage, context.proxy, feature));
	};
	
	/**	
	  Create a new Messenger feature object.
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the messenger handle will be associated with
	  @param {string} [feature=default] the name of the messenger feature
	  @returns {Messenger} an instance of a database object
	*/
	this.messenger = function (context, feature) {
		if (!context) {
			throw "missing argument context";
		}
		feature = this._feature ('messenger', feature);
		return new Messenger (JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Messenger, context.proxy, feature));
	};
	
	/**	
	  Create a new Cache feature object.
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the cache handle will be associated with
	  @param {string} [feature=default] the name of the cache feature
	  @returns {Messenger} an instance of a cache object
	*/
	this.cache = function (context, feature) {
		if (!context) {
			throw "missing argument context";
		}
		feature = this._feature ('cache', feature);
		return new Cache (JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Cache, context.proxy, feature));
	};
	
	/**	
	  Create a new Remote feature object (Http, Coap, Mqtt and others).
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the cache handle will be associated with
	  @param {string} [feature=default] the name of the remote feature
	  @returns {Messenger} an instance of a remote object
	*/
	this.remote = function (context, feature) {
		if (!context || !context.proxy) {
			throw "missing argument context";
		}
		feature = this._feature ('remote', feature);
		return new Remote (JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Remote, context.proxy, feature));
	};
	
	/**	
	  Create a new Indexer feature object.
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the cache handle will be associated with
	  @param {string} [feature=default] the name of the remote feature
	  @returns {Messenger} an instance of a indexer object
	*/
	this.indexer = function (context, feature) {
		if (!context) {
			throw "missing argument context";
		}
		feature = this._feature ('indexer', feature);
		return new Indexer (JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Indexer, context.proxy, feature));
	};
	
	/**	
	  Create a new Scheduler feature object.
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the cache handle will be associated with
	  @param {string} [feature=default] the name of the remote feature
	  @returns {Messenger} an instance of a indexer object
	*/
	this.scheduler = function (context, feature) {
		if (!context) {
			throw "missing argument context";
		}
		feature = this._feature ('scheduler', feature);
		return new Scheduler (JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Scheduler, context.proxy, feature));
	};
	
	/**	
	  Mobile Push notification to a number of recipients (devices)
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the cache handle will be associated with
	  @param {string} [feature=default] the name of the cache feature
	  @param {Array} an array of recipient objects will be receiving the push notification
	  @param {string|Object} the message to send
	*/
	this.push = function (context, feature, recipients, message) {
		if (!context) {
			throw "context is required";
		}
		if (!feature) {
			throw "feature is required";
		}
		if (!message) {
			throw "message is required";
		}
		
		var messenger = new Messenger (JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Messenger, context.proxy, feature));
		
		var sMessage;
		if (typeof message === 'string') {
			sMessage = message;
		} else if (typeof message === 'object') {
			sMessage = JC_ValueConverter.convert (message).toString ();
		}
		
		messenger.send (null, recipients, null, sMessage);
	};
	
	/**	
	  Create a new Shell feature object.
	  If called from the service execute function, the context parameter should be the ApiRequest argument of the execute method
	  If called from the api findCosumer, the context parameter should be the ApiContext
	  @param {ApiContext|ApiRequest} [context] the context or the request in which the cache handle will be associated with
	  @param {string} [feature=default] the name of the cache feature
	  @returns {Messenger} an instance of a cache object
	*/
	this.shell = function (context, feature) {
		if (!context) {
			throw "missing argument context";
		}
		feature = this._feature ('shell', feature);
		return new Shell (JC_FeaturesUtils.feature (proxy.space (), JC_FeaturesUtils.Features.Shell, context.proxy, feature));
	};
	
	/**	
	  Call a service
	  @param {ApiConsumer} consumer - the consumer making the request
	  @param {ApiRequest} request - the parent request
	  @param {JsonObject} spec - the request spec for the target service
	  @returns {ApiOutput} the output
	*/
	this.call = function (consumer, request, spec) {
		if (!consumer) {
			throw "consumer is required";
		}
		if (!request) {
			throw "parent request is required";
		}
		if (!spec) {
			throw "request spec is required";
		}
		return new ApiOutput (JC_ApiUtils.call (proxy, consumer.proxy, request.proxy, JC_ValueConverter.convert (spec)));
	};

	/**	
	  Validate a request
	  @param {ApiConsumer} consumer - the consumer making the request
	  @param {JsonObject} spec - the request spec for the target service
	  @param {ApiRequest} request - the parent request
	*/
	this.validate = function (consumer, spec, request) {
		if (!consumer) {
			throw "consumer is required";
		}
		if (!request) {
			throw "parent request is required";
		}
		if (!spec) {
			throw "request spec is required";
		}
		proxy.validate (consumer.proxy, JC_ValueConverter.convert (spec), request.proxy);
	};

	/**	
	  Create a request
	  @param {ApiConsumer} consumer - the consumer making the request
	  @param {ApiRequest} request - the parent request
	  @param {ApiConsumer} spec - the request spec for the target service
	*/
	this.request = function (consumer, request, spec) {
		if (!consumer) {
			throw "consumer is required";
		}
		if (!request) {
			throw "parent request is required";
		}
		if (!spec) {
			throw "request spec is required";
		}
		return new ApiRequest (JC_ApiUtils.request (proxy, consumer.proxy, request.proxy, JC_ValueConverter.convert (spec), true), true);
	};
	
	this.getRegisteredObject = function (pluginName, objectName) {
		return proxy.space ().getRegisteredObject (pluginName, objectName);
	};

	// private
	this._feature = function (featureType, feature) {
		if (!feature) {
			feature = 'default';
		}
		if (!this.features || !this.features [featureType]) {
			return feature;
		}
		var featureName = feature;
		var lastIndexOfSharp = feature.lastIndexOf ('#');
		if (lastIndexOfSharp > -1) {
			featureName = feature.substring (0, lastIndexOfSharp);
		}
		var found = this.features[featureType][featureName];
		if (!found) {
			return feature;
		}
		if (lastIndexOfSharp > -1) {
			return found + '#' + feature.substring (lastIndexOfSharp + 1);
		}
		return found;
	};

};
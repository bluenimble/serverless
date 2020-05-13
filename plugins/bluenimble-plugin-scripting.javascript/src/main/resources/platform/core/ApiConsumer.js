/**
  A ApiConsumer represents an application, device or user using/calling your api<br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use <br/><br/>
  @class ApiConsumer
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiConsumer = function (proxy) {
	
	this.clazz 		= 'ApiConsumer';

	this.proxy 	= proxy;

	/**	
	  The type of this consumer, resolved by the security method declared in your api<br />
	  <ul>
	  	<li>ApiConsumer.Type.Token</li>
	  	<li>ApiConsumer.Type.Cookie</li>
	  	<li>ApiConsumer.Type.Signature</li>
	  	<li>ApiConsumer.Type.Basic</li>
	  	<li>ApiConsumer.Type.Unknown</li>
	  </ul>
	  @type {string}
	  @readonly
	*/
	this.type 	= proxy.type ().name ();
	
	this.set = function (key, value) {
		proxy.set (key, JC_ValueConverter.convert (value));
		if (key == JC_ApiConsumer_Fields.Id) {
			this.id = this.get (JC_ApiConsumer_Fields.Id);
		}
	};
	
	/**	
	  Get a property of this consumer
	  @param {string} key - the name of the property
	  
	  @returns {Object} the value for the given property
	*/
	this.get = function (key) {
		return proxy.get (key);
	};
	
	/**	
	  The id of this consumer<br />
	  @type {string}
	  @readonly
	*/
	this.id		= this.get (JC_ApiConsumer_Fields.Id);
	
	Object.defineProperty (this, 'id', {
		get: function () { return proxy.get (JC_ApiConsumer_Fields.Id); },
		set: function (value) { proxy.set (JC_ApiConsumer_Fields.Id, value); }
	});
	
	/**	
	  Check if this consumer is anonymous<br />
	  This means, that all security schemes failed to recognize who's making the request<br />
	  This generally, will happen if there is no Authorization header found in the coming request
	  
	  @returns {boolean} 
	*/
	this.isAnonymous = function () {
		return proxy.isAnonymous ();
	};
	
	/**	
	  Changing the anonymous status of this consumer.
	  @param {string} [key=true] - true/false
	*/
	this.setAnonymous = function (a) {
		if (!a) {
			a = true;
		}
		if (!(typeof a === 'boolean')) {
			a = JC_Lang.TrueValues.contains (a);				
		}
		proxy.set (ApiConsumer.Fields.Anonymous, a);
	};
	
	/**	
	  Override this consumer by another one
	  @param {ApiConsumer} reference consumer
	*/
	this.override = function (consumer) {
		return proxy.override (consumer.clazz == 'ApiConsumer' ? consumer.proxy : consumer);
	};
	
	/**	
	  Get a json representation of this consumer
	  
	  @returns {JsonObject} consumer as a json object
	*/
	this.toJson = function () {
		return proxy.toJson ();
	};
	
};
ApiConsumer.prototype.Type = {
	Token: 'Token',
	Signature: 'Signature',
	Basic: 'Basic',
	Cookie: 'Cookie',
	Unknown: 'Unknown'
};
ApiConsumer.prototype.Fields = {
	Type: JC_ApiConsumer_Fields.Type,
	Id: JC_ApiConsumer_Fields.Id,
	Owner: JC_ApiConsumer_Fields.Owner,
	
	Space: JC_ApiConsumer_Fields.Space,
	Role: JC_ApiConsumer_Fields.Role,
	Permissions: JC_ApiConsumer_Fields.AccessKey,

	Token: JC_ApiConsumer_Fields.Token,
	TokenType: JC_ApiConsumer_Fields.TokenType,
	AccessKey: JC_ApiConsumer_Fields.AccessKey,
	SecretKey: JC_ApiConsumer_Fields.SecretKey,
	Signature: JC_ApiConsumer_Fields.Signature,
	ExpiryDate: JC_ApiConsumer_Fields.ExpiryDate,
	Password: JC_ApiConsumer_Fields.Password,
	Anonymous: JC_ApiConsumer_Fields.Anonymous
};
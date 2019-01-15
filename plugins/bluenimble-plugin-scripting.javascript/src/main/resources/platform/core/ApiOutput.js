/**
  Represents a service execution output/result<br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use <br/><br/>
  @class ApiOutput
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiOutput = function (proxy) {
	
	this.clazz 	= 'ApiOutput';
	
	this.proxy 	= proxy;
	
	this.name 			= proxy.name ();
	this.timestamp 		= proxy.timestamp ();
	this.contentType 	= proxy.contentType ();
	this.extension 		= proxy.extension ();
	this.length			= proxy.length ();
	
	this.meta			= proxy.meta ();
	this.data			= proxy.data ();
	
	this.set			= function (key, value) {
		proxy.set (key, value);
		return this;
	};
	this.unset			= function (key) {
		proxy.unset (key);
		return this;
	};
	this.get			= function (key) {
		return proxy.get (key);
	};

	this.pipe			= function (out, position, count) {
		if (typeof position == 'undefined') {
			position = 0;
		}
		if (typeof count == 'undefined') {
			count = -1;
		}
		proxy.pipe (out.proxy, position, count);
	};

	this.toInput		= function () {
		return new InputStream (proxy.toInput ());
	};

};

ApiOutput.prototype.fromBytes = function (name, bytes, contentType, extension) {
	return new JC_ApiByteArrayOutput (name, bytes, contentType, extension);
};
ApiOutput.prototype.fromJson = function (json) {
	return new JC_JsonApiOutput (JC_ValueConverter.convert (json));
};
ApiOutput.prototype.create = function () {
	return new JC_JsonApiOutput (Json.object ());
};
ApiOutput.prototype.Defaults = {
	Id 			: JC_ApiOutput_Defaults.Id,
	Timestamp 	: JC_ApiOutput_Defaults.Timestamp,
	Cache 		: JC_ApiOutput_Defaults.Cache,
	Disposition : JC_ApiOutput_Defaults.Disposition,
	Charset 	: JC_ApiOutput_Defaults.Charset,
	Items 		: JC_ApiOutput_Defaults.Items,
	Count 		: JC_ApiOutput_Defaults.Count,
	Expires		: JC_ApiOutput_Defaults.Expires,
	Cast		: JC_ApiOutput_Defaults.Cast,
	Exit		: JC_ApiOutput_Defaults.Exit
};
ApiOutput.prototype.Disposition = {
	Inline 		: JC_ApiOutput_Disposition.Inline,
	Attachment 	: JC_ApiOutput_Disposition.Attachment
};

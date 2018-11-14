/**
  Represents an api resource located under the resources directory deployed within your api <br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use <br/><br/>
  @class ApiResource
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiResource = function (proxy) {

	this.proxy 			= proxy;

	/**	
	  The name of this resource
	  @type {string}
	  @readonly
	*/
	this.name 			= proxy.name ();
	/**	
	  The path of this resource
	  @type {string}
	  @readonly
	*/
	this.path 			= proxy.path ();
	/**	
	  The timestamp/lastModifiedDate of this resource
	  @type {Date}
	  @readonly
	*/
	this.timestamp 		= proxy.timestamp ();
	/**	
	  The contentType of this resource if recognized by the container
	  @type {string}
	  @readonly
	*/
	this.contentType 	= proxy.contentType ();
	/**	
	  The length/size, ie number of bytes of the content of this resource
	  @type {integer}
	  @readonly
	*/
	this.length 		= proxy.length ();
	/**	
	  The extension of this resource in case if it's a file
	  @type {integer}
	  @readonly
	*/
	this.extension 		= proxy.extension ();
	
	/**	
	  Template the content of this resource using the data provided

	  @returns {Object|string} the content
	*/
	this.template = function (data) {
		return proxy.template (JC_ValueConverter.convert (data));
	};

	/**	
	  Get a StreamSource instance from this resource<br/>
	  The return of this function will undefined in case it's a folder

	  @returns {ApiStreamSource} the stream source
	*/
	this.toStream = function () {
		return new ApiStreamSource (proxy.toStreamSource ());
	};
	
	/**	
	  Get an ApiOutput instance from this resource<br/>

	  @returns {ApiOutput} an api output
	*/
	this.toOutput = function () {
		return new ApiOutput (new JC_ResourceApiOutput (proxy));
	};
	
	/**	
	  Get the children of this resource using a selector<br/>
	  The selector will not apply if this resource represents a file.<br/> 
	  Only resources representing folders may have children (files and folders)
	*/
	this.children = function (selector) {
		var JSelector = Java.extend (JC_ApiResource_Selector, {
		    children: selector
		});
		return proxy.children (new JSelector ());
	};
	
};
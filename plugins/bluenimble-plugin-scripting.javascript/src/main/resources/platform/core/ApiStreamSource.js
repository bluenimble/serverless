/**
  Represents a stream or file <br/>
  <strong>Do not call constructor directly</strong><br/>
  @class ApiStreamSource
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiStreamSource = function (proxy) {

	this.proxy 			= proxy;

	/**	
	  The id of this stream - if any - 
	  @type {string}
	  @readonly
	*/
	this.id			= proxy ? proxy.id () : null;
	/**	
	  The name of this stream - if any - 
	  @type {string}
	  @readonly
	*/
	this.name			= proxy ? proxy.name () : null;
	/**	
	  The contentType of this stream - if any - 
	  @type {string}
	  @readonly
	*/
	this.contentType	= proxy ? proxy.contentType () : null;
	/**	
	  The content of this stream<br/>
	  @type {InputStream}
	  @readonly
	*/
	this.content		= (proxy && proxy.stream ()) ? new InputStream (proxy.stream ()) : null;
	
};
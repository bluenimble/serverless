/**
  Represents an instance of an OutputStream<br/>
  <strong>Do not call constructor directly</strong><br/>
  @class OutputStream
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var OutputStream = function (proxy) {

	this.proxy = proxy;
	
	/**	
	  Writes a single byte
	  @param {byte} - the single byte to write to this OutputStream
	   
	  @returns {OutputStream} the OutputStream instance
	*/
	this.write = function (b) {
		proxy.write (b);
		return this;
	};	
	
	/**	
	  Writes an array of bytes
	  @param {byte[]} - the array of bytes to write to this OutputStream
	   
	  @returns {OutputStream} the OutputStream instance
	*/
	this.writeBytes = function (bytes) {
		proxy.write (bytes);
		this.flush ();
		return this;
	};	
	
	/**	
	  Writes an array of bytes from an offset with a specific length
	  @param {byte[]} - the array of bytes to write to this OutputStream
	  @param {integer} - from offset 
	  @param {integer} - with length 
	   
	  @returns {OutputStream} the OutputStream instance
	*/
	this.write = function (bytes, off, len) {
		proxy.write (bytes, off, len);
		this.flush ();
		return this;
	};	
	
	/**	
	  Writes a string/text using a specific charset
	  @param {string} - the string to write to this OutputStream
	  @param {charset} [charset=utf-8] - the charset to use
	   
	  @returns {OutputStream} the OutputStream instance
	*/
	this.writeText = function (s, charset) {
		if (!s) {
			return;
		}
		if (!charset) {
			charset = 'UTF-8';
		}
		this.writeBytes (new JC_String (s).getBytes (charset));
		return this;
	};	
	
	/**	
	  Force flushing this OutputStream<br/>
	  Note that {@link OutputStream/write} and {@link OutputStream/writeBytes} already flushing when writing to the OutputStream	
	  @returns {OutputStream} the OutputStream instance
	*/
	this.flush = function () {
		proxy.flush ();
		return this;
	};	
	
	/**	
	  Close this instance of OutputStream
	*/
	this.close = function () {
		proxy.close ();
	};	
	
};

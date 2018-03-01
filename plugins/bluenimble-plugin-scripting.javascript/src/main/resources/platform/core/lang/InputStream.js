/**
  Represents an instance of an InputStream<br/>
  <strong>Do not call constructor directly</strong><br/>
  @class InputStream
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var InputStream = function (proxy) {
	
	this.proxy = proxy;
	
	/**	
	  Reads the next single byte
	   
	  @returns {byte} the next single byte 
	*/
	this.read = function () {
		return proxy.read ();
	};
	
	/**	
	  Reads an array of single from an offset with a specific length
	   
	  @returns {byte[]} the array of byte 
	*/
	this.readBytes = function (off, len) {
		var bytes = new JC_Byte [len];
		proxy.read (bytes, off, len);
		return bytes;
	};
	
	/**	
	  Reads a string/text of from an offset with a specific length
	   
	  @returns {string} the resulting string 
	*/
	this.readText = function (off, len) {
		return new JC_String (this.readChunk (off, len));
	};
	
	/**	
	  Fully reads/convert the InputStream content to a string
	   
	  @returns {string} the content of this stream
	*/
	this.toText = function () {
		return JC_IOUtils.toString (proxy);
	};
	
	/**	
	  Skips a number of bytes
	  @param {integer} the number of bytes to be skipped.
	   
	  @returns {string} the actual number of bytes skipped.
	*/
	this.skip = function (n) {
		return proxy.skip (n);
	};
	
	
	
	/**	
	Returns an estimate of the number of bytes that can be read (or skipped over) from this input stream without blocking by the next invocation of a method for this input stream. The next invocation might be the same thread or another thread. A single read or skip of this many bytes will not block, but may read or skip fewer bytes.<br/>
    Note that while some implementations of InputStream will return the total number of bytes in the stream, many will not. It is never correct to use the return value of this method to allocate a buffer intended to hold all data in this stream.
	<br/><br/>
    A subclass' implementation of this method may choose to throw an IOException if this input stream has been closed by invoking the close() method.
	<br/><br/>
	   
	  @returns {string} an estimate of the number of bytes that can be read (or skipped over) from this input stream without blocking or 0 when it reaches the end of the input stream.
	*/
	this.available = function () {
		return proxy.available ();
	};
	
	/**	
	  Closes this InputStream instance
	*/
	this.close = function () {
		proxy.close ();
	};
	
	/**	
Marks the current position in this input stream. A subsequent call to the reset method repositions this stream at the last marked position so that subsequent reads re-read the same bytes.<br/>
The readlimit arguments tells this input stream to allow that many bytes to be read before the mark position gets invalidated.<br/>
<br/>
The general contract of mark is that, if the method markSupported returns true, the stream somehow remembers all the bytes read after the call to mark and stands ready to supply those same bytes again if and whenever the method reset is called. However, the stream is not required to remember any data at all if more than readlimit bytes are read from the stream before reset is called.
<br/><br/>
Marking a closed stream should not have any effect on the stream.
	
	  @param {integer} - the maximum limit of bytes that can be read before the mark position becomes invalid.
   
	  @returns {string} an estimate of the number of bytes that can be read (or skipped over) from this input stream without blocking or 0 when it reaches the end of the input stream.
	*/
	this.mark = function (readlimit) {
		if (!this.markSupported ()) {
			throw 'mark is not supported';
		}
		proxy.mark (readlimit);
	};
	
	/**	
	  Repositions this stream to the position at the time the mark method was last called on this input stream.
	*/
	this.reset = function () {
		proxy.reset ();
	};
	
	/**	
	Tests if this input stream supports the mark and reset methods. Whether or not mark and reset are supported is an invariant property of a particular input stream instance. The markSupported method of InputStream returns false.
	   
	  @returns {boolean} true if this stream instance supports the mark and reset methods; false otherwise.true if this stream instance supports the mark and reset methods; false otherwise.an estimate of the number of bytes that can be read (or skipped over) from this input stream without blocking or 0 when it reaches the end of the input stream.
	*/
	this.markSupported = function () {
		return proxy.markSupported ();
	};
	
};

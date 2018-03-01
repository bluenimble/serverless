/**
  Represents the tracer of an api running in your space 
  <strong>Do not call constructor directly</strong>
  Bluenimble container creates it internally and makes it available for your services to use 	
  @class Tracer
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Tracer = function (proxy) {

	/**	
	  Write a debug level message to the log file/pipe
	  @param {Object} message - message to be written
	*/
	this.debug = function (msg) {
		proxy.log (JC_Tracer_Level.Debug, JC_Converters.convert (msg));
	};
	/**	
	  Write an info level message to the log file/pipe
	  @param {Object} message - message to be written
	*/
	this.info = function (msg) {
		proxy.log (JC_Tracer_Level.Info, JC_Converters.convert (msg));
	};
	/**	
	  Write a warning level message to the log file/pipe
	  @param {Object} message - message to be written
	*/
	this.warn = function (msg) {
		proxy.log (JC_Tracer_Level.Warning, JC_Converters.convert (msg));
	};
	/**	
	  Write an error level message to the log file/pipe
	  @param {Object} message - message to be written
	*/
	this.error = function (msg) {
		proxy.log (JC_Tracer_Level.Error, JC_Converters.convert (msg));
	};
	/**	
	  Write a fatal level message to the log file/pipe
	  @param {Object} message - message to be written
	*/
	this.fatal = function (msg) {
		proxy.log (JC_Tracer_Level.Fatal, JC_Converters.convert (msg));
	};
	
};
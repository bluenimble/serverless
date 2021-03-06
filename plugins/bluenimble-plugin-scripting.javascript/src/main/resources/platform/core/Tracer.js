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
	  @param {vArray} args - substitution arguments
	*/
	this.debug = function () {
		var args 	= Array.prototype.slice.call (arguments);
		args.unshift (JC_Tracer_Level.Debug);
		this._log (args);
	};
	/**	
	  Write an info level message to the log file/pipe
	  @param {Object} message - message to be written
	  @param {vArray} args - substitution arguments
	*/
	this.info = function () {
		var args 	= Array.prototype.slice.call (arguments);
		args.unshift (JC_Tracer_Level.Info);
		this._log (args);
	};
	/**	
	  Write a warning level message to the log file/pipe
	  @param {Object} message - message to be written
	  @param {vArray} args - substitution arguments
	*/
	this.warn = function () {
		var args 	= Array.prototype.slice.call (arguments);
		args.unshift (JC_Tracer_Level.Warning);
		this._log (args);
	};
	/**	
	  Write an error level message to the log file/pipe
	  @param {Object} message - message to be written
	  @param {vArray} args - substitution arguments
	*/
	this.error = function () {
		var args 	= Array.prototype.slice.call (arguments);
		args.unshift (JC_Tracer_Level.Error);
		this._log (args);
	};
	/**	
	  Write a fatal level message to the log file/pipe
	  @param {Object} message - message to be written
	  @param {vArray} args - substitution arguments
	*/
	this.fatal = function () {
		var args 	= Array.prototype.slice.call (arguments);
		args.unshift (JC_Tracer_Level.Fatal);
		this._log (args);
	};
	
	/**	
	  Write a log line with a specific level to the log file/pipe
	  @param {string} level - log level
	  @param {Object} message - message to be written
	  @param {vArray} args - substitution arguments
	*/
	this.log = function () {
		var args = Array.prototype.slice.call (arguments);
		
		var level = JC_Tracer_Level.Info;
		try {
			level = JC_Tracer_Level.valueOf (args [0]);
		} catch (e) {}
		args [0] = level;
		this._log (args);
	};
	
	// private
	this._log = function (args) {
		if (args == null || args.length < 2) {
			throw 'wrong number of arguments';
		}
		var level = args [0];
		var message = args [1];
		
		// remove level and message from args
		args.splice (0, 2);
		
		if (args.length > 0 && args [0].printStackTrace) {
			// Level level, Object o, Throwable th
			proxy['log(com.bluenimble.platform.api.tracing.Tracer.Level,java.lang.Object,java.lang.Throwable)'](level, JC_ValueConverter.convert (message), args [0]);
		} else {
			proxy.log (level, JC_ValueConverter.convert (message), Java.to (args, "java.lang.Object[]"));
		}
	};
	
};

Tracer.prototype.Level = {
	Fatal: 'Fatal',
	Error: 'Error',
	Warning: 'Warning',
	Info: 'Info',
	Debug: 'Debug'
};
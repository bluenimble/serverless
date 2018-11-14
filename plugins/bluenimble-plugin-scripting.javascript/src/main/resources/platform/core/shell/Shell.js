/**
  Represents a Shell instance<br/>
  <strong>Do not call constructor directly</strong>. 
  @see Api#shell
  @class Shell
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Shell = function (proxy) {

	/**	
	  Execute a shell command
	  @param {string} - command
	  @param {object} - parameters

	  @return {JsonObject} - result
	*/
	this.run = function (command, params) {
		return proxy.run (command, JC_ValueConverter.convert (params));
	};
	
};
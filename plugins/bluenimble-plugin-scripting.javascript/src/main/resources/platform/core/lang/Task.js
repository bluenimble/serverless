/**
  Represents an async task<br/>
  @class Task
  @classdesc 
*/

/**
  @constructor
  @param {Api} api - current api
  @param {Function} fn - the function to execute
  @param {Object} options - additional options
*/
var Task = function (api, fn, options) {
	
	if (!options) {
		options = {};
	}
	
	this.clazz 		= 'Task';
	
	this.Callable = Java.extend (JC_Callable, {
		run: function (context) {
			if (Lang.isInteger (options.delay)) {
				JC_Thread.sleep (parseInt (options.delay));
			}
			fn (new ApiContext (context));
		}
	});
	
	/**	
	  Start task execution
	*/
	this.start = function () {
		api.proxy.space ().executor ().execute (new this.Callable (), JC_CodeExec_Mode.Async);
	};	
	
};
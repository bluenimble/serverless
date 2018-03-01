/**
  Lang Utility Class
  @namespace Lang
*/
var Thread = {
	/**	
	  Sleep for a period of time
	  @param {integer} [time=500] - the amount of milliseconds
	*/
	sleep: function (time) {
		if (typeof time == 'undefined') {
			time = 500;
		}
		if (!Lang.isInteger (time)) {
			time = 500;
		}
		return JC_Thread.sleep (time);
	}
	
};
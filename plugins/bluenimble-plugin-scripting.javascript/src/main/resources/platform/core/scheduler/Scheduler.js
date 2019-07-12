/**
  Represents a Scheduler instance<br/>
  <strong>Do not call constructor directly</strong>. 
  @see Api#scheduler
  @class Scheduler
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Scheduler = function (proxy) {

	/**	
	  Start this Scheduler. Starts all its jobs
	  @example
	  
	  api.scheduler (request).start ();
	*/
	this.start = function () {
		return proxy.start ();
	};
	
	/**	
	  Stop this Scheduler. Stops all its jobs
	  @example
	  
	  api.scheduler (request).start ();
	*/
	this.stop = function () {
		return proxy.stop ();
	};
	
	/**	
	  Schedule a Job
	  @param {string} - job id
	  @param {string} - trigger expression
	  @param {Object} - service spec
	  @param {boolean} - save
	  @example
	  
		1. Seconds
	    2. Minutes
	    3. Hours
	    4. Day-of-Month
	    5. Month
	    6. Day-of-Week
	    7. Year (optional field)
	    **Expression**     **Meaning**
	    0 0 12 * * ?     Fire at 12pm (noon) every day
	    0 15 10 ? * *     Fire at 10:15am every day
	    0 15 10 * * ?     Fire at 10:15am every day
	    0 15 10 * * ? *     Fire at 10:15am every day
	    0 15 10 * * ? 2005     Fire at 10:15am every day during the year 2005
	    0 * 14 * * ?     Fire every minute starting at 2pm and ending at 2:59pm, every day
	    0 0/5 14 * * ?     Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day
	    0 0/5 14,18 * * ?     Fire every 5 minutes starting at 2pm and ending at 2:55pm, AND fire every 5 minutes starting at 6pm and ending at 6:55pm, every day
	    0 0-5 14 * * ?     Fire every minute starting at 2pm and ending at 2:05pm, every day
	    0 10,44 14 ? 3 WED     Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.
	    0 15 10 ? * MON-FRI     Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday
	    0 15 10 15 * ?     Fire at 10:15am on the 15th day of every month
	    0 15 10 L * ?     Fire at 10:15am on the last day of every month
	    0 15 10 L-2 * ?     Fire at 10:15am on the 2nd-to-last last day of every month
	    0 15 10 ? * 6L     Fire at 10:15am on the last Friday of every month
	    0 15 10 ? * 6L     Fire at 10:15am on the last Friday of every month
	    0 15 10 ? * 6L 2002-2005     Fire at 10:15am on every last friday of every month during the years 2002, 2003, 2004 and 2005
	    0 15 10 ? * 6#3     Fire at 10:15am on the third Friday of every month
	    0 0 12 1/5 * ?     Fire at 12pm (noon) every 5 days every month, starting on the first day of the month.
	    0 11 11 11 11 ?     Fire every November 11th at 11:11am.
    	  
	  api.scheduler (request).schedule ('Job-001', '* * *', {
        endpoint: 'https://my-task-executor-server',
        data: {
        	a: 'avalue'
        }
	  }, true);
	*/
	this.schedule = function (jobId, expression, service, save) {
		if (typeof save === 'undefined' || save === null) {
			save = false;
		}
		return proxy.schedule (jobId, expression, JC_ValueConverter.convert (service), save);
	};
	
	/**	
	  Schedule a Job
	  @param {string} - job id
	  @param {boolean} - save
	  @example
	  
	  api.scheduler (request).unschedule ('Job-001', true);
	*/
	this.unschedule = function (jobId, save) {
		if (typeof save === 'undefined' || save === null) {
			save = false;
		}
		return proxy.unschedule (jobId, save);
	};

	/**	
	  Pause a Job
	  @param {string} - job id
	  @param {boolean} - save
	  @example
	  
	  api.scheduler (request).pause ('Job-001', true);
	*/
	this.pause = function (jobId, save) {
		if (typeof save === 'undefined' || save === null) {
			save = false;
		}
		return proxy.pause (jobId, save);
	};

	/**	
	  Pause a Job
	  @param {string} - job id
	  @param {boolean} - save
	  @example
	  
	  api.scheduler (request).resume ('Job-001', true);
	*/
	this.resume = function (jobId, save) {
		if (typeof save === 'undefined' || save === null) {
			save = false;
		}
		return proxy.resume (jobId, save);
	};
	
	/**	
	  Get a Job
	  @param {string} - job id
	  @example
	  
	  api.scheduler (request).get ('Job-001');
	*/
	this.get = function (jobId) {
		return proxy.get (jobId);
	};
	
	/**	
	  List Job
	  @param {number} - offset
	  @param {number} - count
	  @param {number} - status. 0: Paused, 1: Running, 2: All
	  @param {boolean} - save
	  @example
	  
	  api.scheduler (request).list (1, 100, 2);
	*/
	this.list = function (offset, count, status) {
		if (typeof offset === 'undefined' || offset === null) {
			offset = 0;
		}
		if (typeof count === 'undefined' || count === null) {
			count = 50;
		}
		if (typeof status === 'undefined' || status === null) {
			status = 2;
		}
		return proxy.list (offset, count, status);
	};

};
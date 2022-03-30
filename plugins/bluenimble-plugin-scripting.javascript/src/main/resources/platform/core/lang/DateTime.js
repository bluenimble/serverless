/**
  Represents an instance of a date <br/>
  <strong>Do not call constructor directly</strong><br/>
  Instances of LocalDateTime are created using the static DateTime class
  @see DateTime 
  @class LocalDateTime
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var LocalDateTime = function (proxy) {
	
	this.proxy 		= proxy;
	
	this.clazz 		= 'LocalDateTime';
	
	/**	
	  Converts this date-time to the number of mili seconds from the epoch of 1970-01-01T00:00:00Z.
	  @param {string} offsetId - time zone offset to use for the conversion
	  
	  @returns {LocalDateTime} Returns the number of seconds from the epoch of 1970-01-01T00:00:00Z
	*/
	this.toEpoch = function (offsetId) {
		var offset;
		if (offsetId) {
			offset = JC_ZoneOffset.of (offsetId);
		} else {
			offset = JC_ZoneOffset.UTC;
		}
		return proxy.toInstant (offset).toEpochMilli ();
	};
	/**	
	  Adding a number of days to this Date instance
	  @param {integer} number of days to be added
	  
	  @returns {LocalDateTime} Returns a copy of this date time plus the specified number of days.
	*/
	this.plusDays = function (amount) {
		return new LocalDateTime (proxy.plusDays (amount));
	};
	/**	
	  Adding a number of hours to this Date instance
	  @param {integer} number of hours to be added
	  
	  @returns {LocalDateTime} Returns a copy of this datetime plus the specified number of hours.
	*/
	this.plusHours = function (amount) {
		return new LocalDateTime (proxy.plusHours (amount));
	};
	/**	
	  Adding a number of minutes to this Date instance
	  @param {integer} number of minutes to be added
	  
	  @returns {LocalDateTime} Returns a copy of this datetime plus the specified number of minutes.
	*/
	this.plusMinutes = function (amount) {
		return new LocalDateTime (proxy.plusMinutes (amount));
	};
	/**	
	  Adding a number of nanoSeconds to this Date instance
	  @param {integer} number of nanoSeconds to be added
	  
	  @returns {LocalDateTime} Returns a copy of this datetime plus the specified number of nanoseconds.
	*/
	this.plusNanos = function (amount) {
		return new LocalDateTime (proxy.plusNanos (amount));
	};
	/**	
	  Adding a number of seconds to this Date instance
	  @param {integer} number of seconds to be added
	  
	  @returns {LocalDateTime} Returns a copy of this datetime plus the specified number of seconds.
	*/
	this.plusSeconds = function (amount) {
		return new LocalDateTime (proxy.plusSeconds (amount));
	};
	/**	
	  Adding a number of weeks to this Date instance
	  @param {integer} number of weeks to be added
	  
	  @returns {LocalDateTime} Returns a copy of this datetime plus the specified number of weeks.
	*/
	this.plusWeeks = function (amount) {
		return new LocalDateTime (proxy.plusWeeks (amount));
	};
	/**	
	  Adding a number of months to this Date instance
	  @param {integer} number of months to be added
	  
	  @returns {LocalDateTime} Returns a copy of this datetime plus the specified number of months.
	*/
	this.plusMonths = function (amount) {
		return new LocalDateTime (proxy.plusMonths (amount));
	};
	/**	
	  Adding a number of years to this Date instance
	  @param {integer} number of years to be added
	  
	  @returns {LocalDateTime} Returns a copy of this datetime plus the specified number of years.
	*/
	this.plusYears = function (amount) {
		return new LocalDateTime (proxy.plusYears (amount));
	};
	/**	
	  Subtracting a number of days from this Date instance
	  @param {integer} number of days to be subtracted
	  
	  @returns {LocalDateTime}  Returns a copy of this datetime minus the specified number of days.
	*/
	this.minusDays = function (amount) {
		return new LocalDateTime (proxy.minusDays (amount));
	};
	/**	
	  Subtracting a number of hours from this Date instance
	  @param {integer} number of hours to be subtracted
	  
	  @returns {LocalDateTime}  Returns a copy of this datetime minus the specified number of hours.
	*/
	this.minusHours = function (amount) {
		return new LocalDateTime (proxy.minusHours (amount));
	};
	/**	
	  Subtracting a number of minutes from this Date instance
	  @param {integer} number of minutes to be subtracted
	  
	  @returns {LocalDateTime}  Returns a copy of this datetime minus the specified number of minutes.
	*/
	this.minusMinutes = function (amount) {
		return new LocalDateTime (proxy.minusMinutes (amount));
	};
	/**	
	  Subtracting a number of nanoSeconds from this Date instance
	  @param {integer} number of nanoSeconds to be subtracted
	  
	  @returns {LocalDateTime}  Returns a copy of this datetime minus the specified number of nanoseconds.
	*/
	this.minusNanos = function (amount) {
		return new LocalDateTime (proxy.minusNanos (amount));
	};
	/**	
	  Subtracting a number of seconds from this Date instance
	  @param {integer} number of seconds to be subtracted
	  
	  @returns {LocalDateTime}  Returns a copy of this datetime minus the specified number of seconds.
	*/
	this.minusSeconds = function (amount) {
		return new LocalDateTime (proxy.minusSeconds (amount));
	};
	/**	
	  Subtracting a number of weeks from this Date instance
	  @param {integer} number of weeks to be subtracted
	  
	  @returns {LocalDateTime}  Returns a copy of this datetime minus the specified number of weeks.
	*/
	this.minusWeeks = function (amount) {
		return new LocalDateTime (proxy.minusWeeks (amount));
	};
	/**	
	  Subtracting a number of months from this Date instance
	  @param {integer} number of months to be subtracted
	  
	  @returns {LocalDateTime}  Returns a copy of this datetime minus the specified number of months.
	*/
	this.minusMonths = function (amount) {
		return new LocalDateTime (proxy.minusMonths (amount));
	};
	/**	
	  Subtracting a number of years from this Date instance
	  @param {integer} number of years to be subtracted
	  
	  @returns {LocalDateTime}  Returns a copy of this datetime minus the specified number of years.
	*/
	this.minusYears = function (amount) {
		return new LocalDateTime (proxy.minusYears (amount));
	};
	/**	
	  Obtains an instance of LocalDateTime from year, month, day of the this date object, with the specified hour, minute and second.
	  @param {integer} hour of the day
	  @param {integer} minute of the hour
	  @param {integer} second of the minute
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the hour, minute and second fields updated.
	*/
	this.withTime = function (hour, minute, second) {
		return new LocalDateTime (JC_LocalDateTime.of (proxy.getYear (), proxy.getMonth (), proxy.getDayOfMonth (), hour, minute, second));
	};
	/**	
	  Create a datetime instance with updated Day Of The Month
	  @param {integer} day of the month
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the day of month field updated.
	*/
	this.withDayOfMonth = function (dayOfMonth) {
		return new LocalDateTime (proxy.withDayOfMonth (dayOfMonth));
	};
	/**	
	  Create a datetime instance with updated Day Of The Year
	  @param {integer} day of the year
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the day of year field updated.
	*/
	this.withDayOfYear = function (dayOfYear) {
		return new LocalDateTime (proxy.withDayOfYear (dayOfYear));
	};
	/**	
	  Create a datetime instance with updated Hour of Day
	  @param {integer} hour of the day
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the hour of day field updated.
	*/
	this.withHour = function (hour) {
		return new LocalDateTime (proxy.withHour (hour));
	};
	/**	
	  Create a datetime instance with updated Minute Of Hour
	  @param {integer} minute of the hour
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the minute of hour field updated.
	*/
	this.withMinute = function (minute) {
		return new LocalDateTime (proxy.withMinute (minute));
	};
	/**	
	  Create a datetime instance with updated Month Of Year
	  @param {integer} month of the year
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the month of year field updated.
	*/
	this.withMonth = function (month) {
		return new LocalDateTime (proxy.withMonth (month));
	};
	/**	
	  Create a datetime instance with updated Nano Of Second
	  @param {integer} nano of the second
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the nano of second field updated.
	*/
	this.withNano = function (nanoOfSecond) {
		return new LocalDateTime (proxy.withNano (nanoOfSecond));
	};
	/**	
	  Create a datetime instance with updated Second Of Minute
	  @param {integer} second of the minute
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the second of minute field updated.
	*/
	this.withSecond = function (second) {
		return new LocalDateTime (proxy.withSecond (second));
	};
	/**	
	  Create a datetime instance with updated year
	  @param {integer} the year
	  
	  @returns {LocalDateTime} Returns a copy of this datetime with the year field updated.
	*/
	this.withYear = function (year) {
		return new LocalDateTime (proxy.withYear (year));
	};
	/**	
	  Gets the day of the month.
	  @returns {integer} Returns the day-of-month.
	*/
	this.dayOfMonth = function () {
		return proxy.getDayOfMonth ();
	};
	/**	
	  Gets the day of the week.
	  @returns {integer} Returns the day of the week.
	*/
	this.dayOfWeek = function () {
		return proxy.getDayOfWeek ().getValue ();
	};
	/**	
	  Gets the day of the year.
	  @returns {integer} Returns the day of the year.
	*/
	this.dayOfYear = function () {
		return proxy.getDayOfYear ();
	};
	/**	
	  Gets the hour of the day.
	  @returns {integer} Returns the hour of the day.
	*/
	this.hour = function () {
		return proxy.getHour ();
	};
	/**	
	  Gets the minute of the hour.
	  @returns {integer} Returns the minute of the hour.
	*/
	this.minute = function () {
		return proxy.getMinute ();
	};
	/**	
	  Gets the month of the year.
	  @returns {integer} Returns the month of the year.
	*/
	this.month = function () {
		return proxy.getMonthValue ();
	};
	/**	
	  Gets the nano of second.
	  @returns {integer} Returns the nano of second.
	*/
	this.nano = function () {
		return proxy.getNano ();
	};
	/**	
	  Gets the second of the minute.
	  @returns {integer} Returns the second of the minute.
	*/
	this.second = function () {
		return proxy.getSecond ();
	};
	/**	
	  Gets the year of this date instace.
	  @returns {integer} Returns the year of this date instace.
	*/
	this.year = function () {
		return proxy.getYear ();
	};
	/**	
	  Format this datetime instance
	  @param {string} the format to use
	  
	  @returns {string} a formatted date string
	*/
	this.format = function (format, language) {
		if (language) {
			return proxy.format (JC_DateTimeFormatter.ofPattern (format, new JC_Locale (language)));
		}
		return proxy.format (JC_DateTimeFormatter.ofPattern (format));
	};
	/**	
	  Check if this datetime instance is after the date in argument 
	  @param {LocalDateTime} the date to be compared to
	  
	  @returns {boolean} true if this datetime instance is after the datetime in argument
	*/
	this.isAfter = function (date) {
		return proxy.isAfter (date.proxy);
	};
	/**	
	  Check if this datetime instance is before the date in argument 
	  @param {LocalDateTime} the date to be compared to
	  
	  @returns {boolean} true if this datetime instance is before the datetime in argument
	*/
	this.isBefore = function (date) {
		return proxy.isBefore (date.proxy);
	};
	/**	
	  Check if this datetime instance is equal to the date in argument 
	  @param {LocalDateTime} the date to be compared to
	  
	  @returns {boolean} true if this datetime instance is equal to the datetime in argument
	*/
	this.isEqual = function (date) {
		return proxy.isEqual (date.proxy);
	};
	/**	
	  Calculates the amount of time until another date-time in terms of the specified unit.
	  @param {LocalDateTime} the the end date-time
	  
	  @returns {integer} the amount of time between this date-time and the end date-time
	*/
	this.until = function (endDate, unit) {
		if (endDate == null || typeof endDate == 'undefined') {
			return 0;
		}
		var jUnit = JC_ChronoUnit.DAYS; 
		if (unit != null && typeof unit != 'undefined') {
			try {
				jUnit = JC_ChronoUnit.valueOf (unit.toUpperCase ());
			} catch (e) {
				// ignore
			}
		}
		return proxy.until (endDate.proxy, jUnit);
	};
	/**	
	  Compare this datetime instance to the date in argument 
	  @param {LocalDateTime} the date to be compared to
	  
	  @returns {integer} negative if less, positive if greater, 0 if they are equal
	*/
	this.compareTo = function (date) {
		return proxy.compareTo (date.proxy);
	};
};

/**
  DateTime Utility Class
  @namespace DateTime
*/
var DateTime = {
	
	UTC: JC_Lang.UTC_DATE_FORMAT,
	
	/**	
	  Get a LocalDateTime from proxy 
	  @param {object} - proxy
	  
	  @returns {LocalDateTime} a date instance 
	*/
	wrap: function (proxy) {
		return new LocalDateTime (proxy);
	},
	/**	
	  Get the current date in a specific time zone 
	  @param {string} [timezone] - the timezone of the resulting date
	  
	  @returns {LocalDateTime} a date instance 
	*/
	now: function (zone) {
		if (!zone) {
			zone = 'UTC';
		}
		return new LocalDateTime (JC_LocalDateTime.now (JC_ZoneId.of (zone)));
	},
	/**	
	  Get a date object corresponding to the timestamp in a specific time zone 
	  @param {integer} [timestamp] - the integer timestamp
	  @param {string} [timezone] - the timezone of the resulting date
	  
	  @returns {LocalDateTime} a date instance 
	*/
	withTimestamp: function (timestamp, zone) {
		if (!zone) {
			zone = 'UTC';
		}
		return new LocalDateTime ( JC_LocalDateTime.ofInstant (JC_Instant.ofEpochMilli (timestamp), JC_ZoneId.of (zone)) ); 
	},
	/**	
	  Get a date object corresponding to the native date 
	  @param {Object} [date] - the date object
	  
	  @returns {LocalDateTime} a datetime instance 
	*/
	withDate: function (date, zone) {
		if (!date || date == null) {
			return;
		}
		if (date.getClass ().getName () == 'java.util.Date') {
			if (!zone) {
				zone = 'UTC';
			}
			return new LocalDateTime ( JC_LocalDateTime.ofInstant (JC_Instant.ofEpochMilli (date.getTime ()), JC_ZoneId.of (zone)) ); 
		} else if (date.getClass ().getName () == 'java.time.LocalDateTime') {
			return new LocalDateTime (date);
		} 
		return date;
	},
	/**	
	  Creates a LocalDateTime instance by parsing a string using a specific date format
	  @param {string} [sdate] - string representation of a date
	  @param {string} [format] - date format
	  
	  @returns {LocalDateTime} a date instance 
	*/
	parse: function (sdate, format) {
		if (!sdate) {
			throw "date string shouldn't be null";
		}
		if (!format) {
			return new LocalDateTime (JC_LocalDateTime.parse (sdate));
		}
		return new LocalDateTime (JC_LocalDateTime.parse (sdate, JC_DateTimeFormatter.ofPattern (format)));
	},
	/**	
	  Creates a LocalDateTime instance using year, month, day, hour, minute, second, nanosecond
	  @param {integer} date year
	  @param {integer} date month
	  @param {integer} date day
	  @param {integer} date hour
	  @param {integer} date minute
	  @param {integer} [second] - date second
	  @param {integer} [nanosecond] - date nanosecond
	  
	  @returns {LocalDateTime} a date instance 
	*/
	of: function () {
		var args 			= Array.prototype.slice.call (arguments);
		if (!args || args.length < 5) {
			throw 'wrong number of arguments -> (year, month, day, hour, minute, [seconds], [nanoSeconds])';
		}
		var year 	= args [0];
 		var month	= args [1];
 		var day		= args [2];
		var hour	= args [3]; 
		var minute	= args [4];
		
		var jc_ldt;
		
		if (args.length == 5) {
			jc_ldt = JC_LocalDateTime.of (year, month, day, hour, minute);
		} else if (args.length == 6) {
			var second			= args [5]; 
			jc_ldt = JC_LocalDateTime.of (year, month, day, hour, minute, second);
		} else if (args.length > 6) {
			var nanoOfSecond	= args [6];
			jc_ldt = JC_LocalDateTime.of (year, month, day, hour, minute, second, nanoOfSecond);
		}
		
		return new LocalDateTime (jc_ldt);
	},
	/**	
	  Format a Date object in UTC yyyy-MM-dd'T'HH:mm:ss'Z'
	  @param {Date} [date] - the date object
	  @param {string} [format] - date format
	  
	  @returns {string} the utc formatted date
	*/
	utc: function (date) {
		return JC_Lang.toUTC (date);
	},
	/**	
	  Format a Date object using a specific format
	  @param {Date} [date] - the date object
	  @param {string} [format] - date format
	  
	  @returns {LocalDateTime} a date instance 
	*/
	format: function (date, format) {
		return JC_Lang.toString (date, format);
	}
};
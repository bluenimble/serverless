/**
  Represents a Storage instance<br/>
  <strong>Do not call constructor directly</strong>. 
  To create an storage instance
  @see Api#storage
  @class Storage
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var Storage = function (proxy) {

	/**	
	  The mount id
	  @type {string}
	  @readonly
	*/
	this.mount = proxy.mount ();
	
	/**	
	  Get the root folder
	  
	  @returns {Folder} the root folder
	*/
	this.root = function () {
		return new Folder (proxy.root ());
	};
	
};
/**
  Represents the resources manager of an api<br/>
  <strong>Do not call constructor directly</strong><br/>
  Bluenimble container creates it internally and makes it available for your services to use <br/><br/>
  <strong>Resources are folders or file you deploy within your api under the resources directory</strong><br/>
  @class ApiResourcesManager
  @classdesc 
*/

/**
  @constructor
  @access private
*/
var ApiResourcesManager = function (proxy) {

	/**	
	  Get a resource - folder or file - with the given path
	  @param {string} path - the path of the folder or file
	  
	  @returns {ApiResource} the resource for the given path
	*/
	this.get = function (path) {
		var jPath = null;
		if (path) {
			jPath = Java.to (path.split ('/'), 'java.lang.String[]');
		}
		var r = proxy.get (jPath);
		if (!r) {
			throw "storage object '" + path + "' not found";
		}
		return new ApiResource (r);
	};
	
};
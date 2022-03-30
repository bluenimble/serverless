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
var ApiServicesManager = function (proxy) {

	/**	
	  Get a service by id
	  @param {string} id - the id of the service
	  
	  @returns {ApiService} the service for the given id
	*/
	this.get = function (id) {
		var serviceProxy = proxy.getById (id);
		if (serviceProxy == null) {
			return;
		}
		var helper = serviceProxy.getHelper ('SpecAndSpiPair');
		if (helper) {
			return helper.spec ();
		}
		return new ApiService (serviceProxy);
	};
	
};
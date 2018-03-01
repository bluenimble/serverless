return {

	/**
	 * Implement this function in order to authenticate users only if you're using a custom authentication scheme different than the ones provided by default.
	 * 
	 * The execute function will be triggered when an application or device makes a call to any function under this api. 
	 * 
	 * Arguments:
	 *  Api 		 the api where this function is running  
	 *  ApiService   the function specification.
	 *  ApiRequest 	 the parameters, headers and streams. 
	 *               ex. request.get ('email') to get a parameter called 'email'
	 *               request.get ('Token', ApiRequest.Scope.Header) to get an http or CoAp header called 'Token'
	 *               request.get ('myfile', ApiRequest.Scope.Stream) to get a stream called 'myfile' such as uploads 
	 *  ApiConsumer  the user, application, or device calling this api. It could be a ApiConsumer.Type.Token, ApiConsumer.Type.Cookie, ApiConsumer.Type.Signature, 
	 *				 ApiConsumer.Type.Basic or ApiConsumer.Type.Unknown
	 *
	 *	@author		{user}
	 *	@created	{date}
	 * 
	 **/
	findConsumer: function (api, service, request, consumer) {
		/**
		 *
		 * Remove this portion of code if you want to write a custom authentication logic.
		 * This code remains valid only if you're using BlueNimble default authentication schemes see api.json / security section
		 *
		 **/

		/**
		 *
		 * Always authorize if a function is called from another function in the same environment 
		 *
		 **/
		if (request.channel == 'container') {
			consumer.override (
				request.get (ApiRequest.Consumer)
			);
	        return;
	    }
		
	    if (consumer.type == ApiConsumer.Type.Unknown && consumer.isAnonymous () && this.isSecure (service) ) {
			throw 'authentication error'
		}
	},
    
    isSecure: function (service) {
        return !service.security || (typeof service.security.enabled == 'undefined') || service.security.enabled == 'true';
    } 
	
}
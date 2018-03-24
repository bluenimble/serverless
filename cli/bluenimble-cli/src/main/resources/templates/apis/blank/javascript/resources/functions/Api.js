return {

	/**
	 * Implement this function in order to authenticate users only if you're using a custom authentication scheme different than the ones provided by default.
	 * 
	 * The execute function will be triggered when an application or device makes a call to any service under this api. 
	 * 
	 * Arguments:
	 *  Api 		 the api where this service is running  
	 *  ApiService   the service, the application or device is calling.
	 *  ApiRequest 	 the parameters, headers and streams. 
	 *               ex. request.get ('email') to get a parameter called 'email'
	 *               request.get ('Token', ApiRequest.Scope.Header) to get an http or CoAp header called 'Token'
	 *               request.get ('myfile', ApiRequest.Scope.Stream) to get a stream called 'myfile' such as uploads 
	 *  ApiConsumer  the user, application, or device calling this api. It could be a ApiConsumer.Type.Token, ApiConsumer.Type.Cookie, ApiConsumer.Type.Signature, 
	 *				 ApiConsumer.Type.Basic or ApiConsumer.Type.Unknown
	 *
	 *	@author		${user}
	 *	@created	${date}
	 * 
	 **/
	findConsumer: function (api, service, request, consumer) {
        // unsecure api based on blank template
        // you can either create you own authentication algorithm or use BlueNimble out-of-the-box Identity Management plugin
        return;
	} 
	
}
return {
	
	/**
	 * The only required function that you should implement, if no mock data provided in your Delete{Model}.json
	 * 
	 * The execute function will be triggered when an application or device makes a call to {verb} [bluenimble-space].[bluenimble-instance].bluenimble.com/{api}/{models}/:{model} 
	 * which is defined in your service specification file Delete{Model}.json 
	 * 
	 * Arguments:
	 *  Api 		 the api where this service is running  
	 *  ApiConsumer  the user, application, or device calling this service. It could be a ApiConsumer.Type.Token, ApiConsumer.Type.Cookie, ApiConsumer.Type.Signature, 
	 *				 ApiConsumer.Type.Basic or ApiConsumer.Type.Unknown
	 *  ApiRequest 	 the parameters, headers and streams. 
	 *               ex. request.get ('email') to get a parameter called 'email'
	 *               request.get ('Token', ApiRequest.Scope.Header) to get an http or CoAp header called 'Token'
	 *               request.get ('myfile', ApiRequest.Scope.Stream) to get a stream called 'myfile' such as uploads 
	 *  ApiResponse  to set headers to send back to the calling device or application 
	 * 				 response.set ('X-MyHeader', 'Hello');
	 *				 you can also write data to the response but this is rarely will happen as the platform takes care of this.	
	 *
	 *
	 *	@author		{user}
	 *	@created	{date}
	 * 
	 **/
	execute: function (api, consumer, request, response) {
		
		// deleting a {Model} by id (':{model}')
		
		var {model} = api.database (request).get ( '{Models}', request.get ('{model}') );
		if (!{model}) {
			return { deleted: 0, reason: 'NotFound' };
		}
		
		var terminated = false;
		
		if (request.get ('terminate')) {
			terminated = {model}.delete ();
		} else {
			{model}.set ('deleted', true).save ();
		}
		
		return { deleted: (terminated ? 2 : 1) };
		
	}

}
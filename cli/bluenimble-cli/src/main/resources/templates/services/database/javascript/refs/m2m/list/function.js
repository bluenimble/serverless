return {
	
	/**
	 * The only required function that you should implement, if no mock data provided in your List[[Model]][[Refs]].json
	 * 
	 * The execute function will be triggered when an application or device makes a call to post [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]/[[models]]
	 * which is defined in your service specification file List[[Model]][[Refs]].json 
	 * 
	 * Arguments:
	 *  Api 		 the api where this service is running  
	 *  ApiConsumer  the user, application, or device calling this service. It could be a ApiConsumer.Type.Token, ApiConsumer.Type.Cookie, ApiConsumer.Type.Signature, 
	 *				 ApiConsumer.Type.Basic or ApiConsumer.Type.Unknown
	 *  ApiRequest 	 the parameters, headers and streams. 
	 *               ex. request.get ('email') to get a parameter called 'email'
	 *               request.get ('Token', ApiRequest.Scope.Header) to get an http or CoAp header called 'Token'
	 *               request.get ('myfile', ApiRequest.Scope.Stream) to get a stream called 'myfile' (uploads) 
	 *  ApiResponse  to set headers to send back to the calling device or application 
	 * 				 response.set ('X-MyHeader', 'Hello');
	 *				 you can also write data to the response but this is rarely will happen as the platform takes care of this.	
	 *
	 *
	 *	@author		[[user]]
	 *	@created	[[date]]
	 * 
	 **/
	execute: function (api, consumer, request, response) {
		
		// List [[Model]] [[Refs]]
		
		var [[model]]Id =  request.get ('[[model]]');
		
		var db = api.database (request);

		var [[model]] = db.get ( '[[Model]]', [[model]]Id );
		
		if (![[model]]) {
			throw new ApiServiceExecutionException (
				api.message (request.lang, 'NotFound', '[[model]]', [[model]]Id)
			).status (ApiResponse.NOT_FOUND);
		}			
		
		// run query
		var result = { [[refs]]: Json.array () };
		
		var [[refs]] = [[model]].get ('[[refs]]');
		if ([[refs]] == null || [[refs]].isEmpty ()) {
			return result;
		}
		
		for (var i = 0; i < [[refs]].size (); i++) {
			result.[[refs]].push ([[refs]].get (i).toJson (0, 0));
		}
		
		return result;
		
	}

}
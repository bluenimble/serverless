return {
	
	/**
	 * The only required function that you should implement, if no mock data provided in your Update[[Model]].json
	 * 
	 * The execute function will be triggered when an application or device makes a call to [[verb]] [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]/[[models]]/:[[model]]
	 * which is defined in your service specification file Update[[Model]].json 
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
		
		// Update an existing [[Model]]

		var payload = request.get (ApiRequest.Payload);
		
		var [[model]]Id =  request.get ('[[model]]');

		var db = api.database (request);

		// check existence of the [[Model]] given by :[[model]]Id
		var [[model]] = db.get ('[[Models]]', [[model]]Id);
		
		if (![[model]]) {
			throw new ApiServiceExecutionException (
				api.message (request.lang, 'NotFound', '[[model]]', [[model]]Id)
			).status (ApiResponse.NOT_FOUND);
		}
		[[#eq ModelSpec.addDefaults 'true']]// set the current user as the updater of this [[Model]]
		[[model]].ref ('updatedBy', 'Users', consumer.id);[[/eq]]
		
		[[#if ModelSpec.refs]]// resolve references [[#each ModelSpec.refs]][[#neq multiple 'true']]
		if (payload.[[@key]]) {
		[[#eq exists 'true']]
			var [[@key]] = db.get ('[[entity]]', payload.[[@key]].id);	
			if (![[@key]]) {
				throw new ApiServiceExecutionException (
					api.message (request.lang, 'NotFound', '[[@key]]', payload.[[@key]].id)
				).status (ApiResponse.NOT_FOUND);
			}
			[[model]].set ('[[@key]]', [[@key]]);
			
			// remove '[[@key]]' from payload
			payload.remove ('[[@key]]');
		[[else]]
			payload.[[@key]] [Database.Fields.Entity] = '[[entity]]';
		[[/eq]]
		}[[/neq]][[/each]][[/if]]
		
		// load payload into [[model]]
		[[model]].load (payload);
			
		// save [[model]]			
		[[model]].save ();
		
		// return minimal info about created [[model]]		
		return [[model]].toJson (0, 0);

	
	}

}
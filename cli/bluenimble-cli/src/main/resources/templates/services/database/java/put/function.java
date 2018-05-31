package [[package]].[[models]];
	
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;

import com.bluenimble.platform.api.security.ApiConsumer;

import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.DatabaseException;

import com.bluenimble.platform.api.impls.JsonApiOutput;

import com.bluenimble.platform.json.JsonObject;

/**
 * The only required function that you should implement, if no mock data provided in your Update[[Model]].json
 * 
 * The execute function will be triggered when an application or device makes a call to [[verb]] [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]
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
 *	@author		[[user]]
 *	@created	[[date]]
 * 
 **/

public class Update[[Model]] extends AbstractApiServiceSpi {
	
	private static final long serialVersionUID = [[randLong]]L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		// Create a new [[Model]]
		
		JsonObject payload = (JsonObject)request.get (ApiRequest.Payload);

		Object [[model]]Id = request.get ("[[model]]");

		Database db = feature (api, Database.class, null, request);
		
		// get [[Model]] by :[[model]]Id
		DatabaseObject [[model]] = null;
		try {
			[[model]] = db.get ("[[Models]]", [[model]]Id);
		} catch (DatabaseException dbex) {
			throw new ApiServiceExecutionException (dbex.getMessage (), dbex);
		}

		if ([[model]] == null) {
			throw new ApiServiceExecutionException (
				api.message (request.getLang (), "NotFound", "[[model]]", [[model]]Id)
			).status (ApiResponse.NOT_FOUND);
		}
		
		try {
			[[#eq ModelSpec.addDefaults 'true']]// set the current user as the updater of this [[Model]]
			[[model]].set ('updatedBy', new JsonObject ().set (Database.Fields.Entity, "Users").set (Database.Fields.Entity, consumer.get (ApiConsumer.Fields.Id));[[/eq]]
			
			[[#if ModelSpec.refs]]// resolve references [[#each ModelSpec.refs]][[#neq multiple 'true']]if (payload.containsKey ("[[@key]]")) {[[#eq exists 'true']]
				Object [[@key]]Id = Json.find (payload, "[[@key]]", Database.Fields.Id);
				DatabaseObject [[@key]] = db.get ("[[entity]]", [[@key]]Id);
				if ([[@key]] == null) {
					throw new ApiServiceExecutionException (
						api.message (request.getLang (), "NotFound", "[[@key]]", [[@key]]Id)
					).status (ApiResponse.NOT_FOUND);
				}
				[[model]].set ("[[@key]]", [[@key]]);
				
				// remove '[[@key]]' from payload
				payload.remove (""[[@key]]");
			[[else]]Json.getObject (payload, "[[@key]]").set (Database.Fields.Entity, "[[entity]]");[[/eq]]}[[/neq]][[/each]][[/if]]
			
			// load payload into [[model]]
			[[model]].load (payload);
				
			// save [[model]]			
			[[model]].save ();
		} catch (Exception ex) {
			if (ex instanceof ApiServiceExecutionException) {
				throw (ApiServiceExecutionException)ex;
			}
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		// return minimal info about created [[model]]		
		return new JsonApiOutput ([[model]].toJson (null));
	}
	
}
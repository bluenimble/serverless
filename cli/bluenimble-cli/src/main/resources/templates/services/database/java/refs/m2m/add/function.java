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

/**
 * The only required function that you should implement, if no mock data provided in your Add[[Model]][[Ref]].json
 * 
 * The execute function will be triggered when an application or device makes a call to [[verb]] [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]
 * which is defined in your service specification file Add[[Model]][[Ref]].json 
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

public class Add[[Model]][[Ref]] extends AbstractApiServiceSpi {
	
	private static final long serialVersionUID = [[randLong]]L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		// Add a [[Ref]] to [[Model]]
		
		Object [[model]]Id 	= request.get ("[[model]]");
		Object [[ref]]Id 	= request.get ("[[ref]]");
		
		// write to database
		Database db = feature (api, Database.class, null, request);
		
		DatabaseObject [[model]] = null;
		try {
			// lookup [[Model]] by :[[model]]
			[[model]] = db.get ("[[Models]]", [[model]]Id);
		} catch (DatabaseException dbex) {
			throw new ApiServiceExecutionException (dbex.getMessage (), dbex);
		}	
		if ([[model]] == null) {
			throw new ApiServiceExecutionException (
				api.message (request.getLang (), 'NotFound', '[[model]]', [[model]]Id)
			).status (ApiResponse.NOT_FOUND);
		}
		
		DatabaseObject [[ref]] = null;
		try {
			// lookup [[Ref]] by :[[ref]]
			[[ref]] = db.get ("[[RefSpec.entity]]", [[ref]]Id);
		} catch (DatabaseException dbex) {
			throw new ApiServiceExecutionException (dbex.getMessage (), dbex);
		}	
		if ([[ref]] == null) {
			throw new ApiServiceExecutionException (
				api.message (request.getLang (), "NotFound", "[[ref]]", [[ref]]Id)
			).status (ApiResponse.NOT_FOUND);
		}

		DatabaseObject [[model]][[Ref]] = null;
		try {
			// add link
			[[model]][[Ref]] = db.create ("[[Model]][[Refs]]");
			[[model]][[Ref]].set ("[[model]]", [[model]]Id);
			[[model]][[Ref]].set ("[[ref]]", [[ref]]Id);
			
			// save [[model]][[Ref]]
			[[model]][[Ref]].save ();
		} catch (DatabaseException dbex) {
			throw new ApiServiceExecutionException (dbex.getMessage (), dbex);
		}
		
		return new JsonApiOutput ([[model]][[Ref]].toJson (null));
	}
	
}
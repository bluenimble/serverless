package [[package]].[[models]];

import java.util.List;

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
 * The only required function that you should implement, if no mock data provided in your Remove[[Model]][[Ref]].json
 * 
 * The execute function will be triggered when an application or device makes a call to [[verb]] [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]
 * which is defined in your service specification file Remove[[Model]][[Ref]].json 
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

public class Remove[[Model]][[Ref]] extends AbstractApiServiceSpi {
	
	private static final long serialVersionUID = [[randLong]]L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		// Remove [[model]][[Ref]] by :[[model]] and :[[ref]]
		
		Object [[model]]Id 	= request.get ("[[model]]");
		Object [[ref]]Id 	= request.get ("[[ref]]");

		Database db = feature (api, Database.class, null, request);
		
		DatabaseObject [[model]] = null;
		try {
			[[model]] = db.get ("[[Model]]", [[model]]Id);
		} catch (DatabaseException dbex) {
			throw new ApiServiceExecutionException (dbex.getMessage (), dbex);
		}
		
		if ([[model]] == null) {
			throw new ApiServiceExecutionException (
				api.message (request.getLang (), "NotFound", "[[model]]", [[model]]Id)
			).status (ApiResponse.NOT_FOUND);
		}	
		
		@SuppressWarnings("unchecked")
		List<DatabaseObject> [[refs]] = (List<DatabaseObject>)[[model]].get ("[[refs]]");
		if ([[refs]] == null || [[refs]].isEmpty ()) {
			return new JsonApiOutput ((JsonObject)new JsonObject ().set ("removed", false));
		}
		
		DatabaseObject found = null;
		for (DatabaseObject [[ref]] : [[refs]]) {
			if ([[ref]].getId ().equals ([[ref]]Id)) {
				found = [[ref]];
			}
		}
		
		if (found != null) {
			[[refs]].remove (found);
			try {
				// save [[model]]
				[[model]].save ();
			} catch (DatabaseException dbex) {
				throw new ApiServiceExecutionException (dbex.getMessage (), dbex);
			}
		}
		
		return new JsonApiOutput ((JsonObject)new JsonObject ().set ("removed", found != null));
	}
	
}
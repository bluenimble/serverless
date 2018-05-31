package [[package]].[[models]];
	
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.api.ApiServiceExecutionException;

import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;

import com.bluenimble.platform.json.JsonObject;

/**
 * The only required function that you should implement, if no mock data provided in your Get[[Model]][[Ref]].json
 * 
 * The execute function will be triggered when an application or device makes a call to [[verb]] [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]
 * which is defined in your service specification file Get[[Model]][[Ref]].json 
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

public class Get[[Model]][[Ref]]Spi extends AbstractApiServiceSpi {

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		// get [[Ref]] of [[Model]]
		
		Database db = api.space ().feature (Database.class, null, request);
		
		// get [[Model]] by :[[model]]
		DatabaseObject [[model]] = db.get ("[[Models]]", request.get ("[[model]]") );
		
		if ([[model]] == null) {
			throw new ApiServiceExecutionException (
				api.message (request.lang, "NotFound", "[[model]]", [[model]]Id)
			).status (ApiResponse.NOT_FOUND);
		}
		
		DatabaseObject [[ref]] = [[model]].get ("[[ref]]");
		if ([[ref]] == null) {
			return new JsonApiOutput (JsonObject.Blank);
		}
		
		return new JsonApiOutput ([[ref]].toJson ());
		
	}
	
}
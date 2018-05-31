package [[package]].[[models]];

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;

import com.bluenimble.platform.api.security.ApiConsumer;

import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.Database.Visitor;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.query.impls.JsonQuery;

import com.bluenimble.platform.api.impls.JsonApiOutput;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.json.JsonArray;

/**
 * The only required function that you should implement, if no mock data provided in your List[[Model]][[Refs]].json
 * 
 * The execute function will be triggered when an application or device makes a call to [[verb]] [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]
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
 *	@author		[[user]]
 *	@created	[[date]]
 * 
 **/

public class List[[Model]][[Refs]] extends AbstractApiServiceSpi {
	
	private static final long serialVersionUID = [[randLong]]L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		// List [[Model]] [[Refs]] by :filter
		
		Object [[model]]Id 	= request.get ("[[model]]");
		
		// build query

		JsonObject filter = (JsonObject)request.get ("filter");
		
		JsonObject where = (JsonObject)new JsonObject ().set ("[[model]]", [[model]]Id);
		
		if (filter != null) {
			where.putAll (filter);
		}

		// run query
		JsonObject result 		= new JsonObject ();
		JsonArray [[refs]] 		= new JsonArray ();
		result.set ("[[refs]]", [[refs]]);
		
		Database db = feature (api, Database.class, null, request);
		
		try {
			db.find ("[[Model]]_[[Refs]]", new JsonQuery ((JsonObject)new JsonObject ().set ("where", where)), new Visitor () {
				@Override
				public boolean onRecord (DatabaseObject [[model]][[Ref]]) {
					[[refs]].add (((DatabaseObject)[[model]][[Ref]].get ("[[ref]]")).toJson (null));
					return false;
				}
				@Override
				public boolean optimize () { return true; }
			});
		} catch (DatabaseException dbex) {
			throw new ApiServiceExecutionException (dbex.getMessage (), dbex);
		}
		
		return new JsonApiOutput (result);
	}
	
}
package [[package]].[[models]]
	
import com.bluenimble.platform.api.Api
import com.bluenimble.platform.api.ApiOutput
import com.bluenimble.platform.api.ApiRequest
import com.bluenimble.platform.api.ApiResponse
import com.bluenimble.platform.api.ApiServiceExecutionException
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi

import com.bluenimble.platform.api.security.ApiConsumer

import com.bluenimble.platform.api.impls.JsonApiOutput

import com.bluenimble.platform.db.Database
import com.bluenimble.platform.db.Database.Visitor
import com.bluenimble.platform.db.DatabaseObject
import com.bluenimble.platform.db.DatabaseException
import com.bluenimble.platform.db.query.impls.JsonQuery

import com.bluenimble.platform.json.JsonArray
import com.bluenimble.platform.json.JsonObject

/**
 * The only required function that you should implement, if no mock data provided in your Find[[Models]].json
 * 
 * The execute function will be triggered when an application or device makes a call to [[verb]] [bluenimble-space].[bluenimble-instance].bluenimble.com/[[api]]
 * which is defined in your service specification file Find[[Models]].json 
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
 * 				 response.set ('X-MyHeader', 'Hello')
 *				 you can also write data to the response but this is rarely will happen as the platform takes care of this.	
 *
 *	//@author		[[user]]
 *	//@created	[[date]]
 * 
 **/

@SerialVersionUID ([[randLong]]L)
class Find[[Models]] extends AbstractApiServiceSpi {

	@throws (classOf [ApiServiceExecutionException])
	def execute (api: Api, consumer: ApiConsumer, request: ApiRequest, response: ApiResponse): ApiOutput = {
		
		// find [[Models]] by query
		
		var query: JsonObject = request.get ("query").asInstanceOf[JsonObject]
		if (query == null) {
			query = new JsonObject ().set (
				"where",
				new JsonObject ()[[#eq ModelSpec.markAsDeleted 'true']].set ("deleted", false)[[/eq]]
			).asInstanceOf[JsonObject]
		} 
		
		var result: JsonObject 		= new JsonObject ()
		var [[models]]: JsonArray 	= new JsonArray ()
		result.set ("[[models]]", [[models]])
		
		var db: Database = feature (api, classOf[Database], null, request)
		
		try {
			db.find ("[[Model]]", new JsonQuery (query), new Visitor () {

				def onRecord ([[model]]: DatabaseObject): Boolean = {
					[[models]].add ([[model]].toJson (null))
					false
				}

				def optimize (): Boolean = { true }
			})
		} catch {
			case dbex: DatabaseException => throw new ApiServiceExecutionException (dbex.getMessage, dbex)
		}
		
		new JsonApiOutput (result)
	}
	
}
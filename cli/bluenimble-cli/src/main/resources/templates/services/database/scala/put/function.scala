package [[package]].[[models]]
	
import com.bluenimble.platform.api.Api
import com.bluenimble.platform.api.ApiOutput
import com.bluenimble.platform.api.ApiRequest
import com.bluenimble.platform.api.ApiResponse
import com.bluenimble.platform.api.ApiServiceExecutionException
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi

import com.bluenimble.platform.api.security.ApiConsumer

import com.bluenimble.platform.db.Database
import com.bluenimble.platform.db.DatabaseObject
import com.bluenimble.platform.db.DatabaseException

import com.bluenimble.platform.api.impls.JsonApiOutput

import com.bluenimble.platform.json.JsonObject

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
 * 				 response.set ('X-MyHeader', 'Hello')
 *				 you can also write data to the response but this is rarely will happen as the platform takes care of this.	
 *
 *	//@author		[[user]]
 *	//@created	[[date]]
 * 
 **/

@SerialVersionUID ([[randLong]]L)
class Update[[Model]] extends AbstractApiServiceSpi {

	@throws (classOf [ApiServiceExecutionException])
	def execute (api: Api, consumer: ApiConsumer, request: ApiRequest, response: ApiResponse): ApiOutput = {
		
		// Create a new [[Model]]
		
		val payload: JsonObject = request.get (ApiRequest.Payload).asInstanceOf[JsonObject]

		val [[model]]Id: Object = request.get ("[[model]]")

		val db: Database = feature (api, classOf[Database], null, request)
		
		// get [[Model]] by :[[model]]Id
		var  [[model]]: DatabaseObject = null
		try {
			[[model]] = db.get ("[[Model]]", [[model]]Id)
		} catch {
			case dbex: DatabaseException => throw new ApiServiceExecutionException (dbex.getMessage, dbex)
		}

		if ([[model]] == null) {
			throw new ApiServiceExecutionException (
				api.message (request.getLang, "NotFound", "[[model]]", [[model]]Id)
			).status (ApiResponse.NOT_FOUND)
		}
		
		try {
			[[#eq ModelSpec.addDefaults 'true']]// set the current user as the updater of this [[Model]]
			[[model]].set ('updatedBy', new JsonObject ().set (Database.Fields.Entity, "User").set (Database.Fields.Id, consumer.get (ApiConsumer.Fields.Id))[[/eq]]
			
			[[#if ModelSpec.refs]][[#each ModelSpec.refs]][[#neq multiple 'true']]
			if (payload.containsKey ("[[@key]]")) {[[#eq exists 'true']]
				var [[@key]]Id: Object = com.bluenimble.platform.Json.find (payload, "[[@key]]", Database.Fields.Id)
				var [[@key]]: DatabaseObject = db.get ("[[entity]]", [[@key]]Id)
				if ([[@key]] == null) {
					throw new ApiServiceExecutionException (
						api.message (request.getLang, "NotFound", "[[@key]]", [[@key]]Id)
					).status (ApiResponse.NOT_FOUND)
				}
				[[model]].set ("[[@key]]", [[@key]])
				
				// remove '[[@key]]' from payload
				payload.remove ("[[@key]]")
			[[else]]Json.getObject (payload, "[[@key]]").set (Database.Fields.Entity, "[[entity]]")[[/eq]]}[[/neq]][[/each]][[/if]]
			
			// load payload into [[model]]
			[[model]].load (payload)
				
			// save [[model]]			
			[[model]].save ()
		} catch {
			case ex: ApiServiceExecutionException => throw ex.asInstanceOf[ApiServiceExecutionException]
			case other: Throwable => throw new ApiServiceExecutionException (other.getMessage, other)
		}
		
		// return minimal info about created [[model]]		
		new JsonApiOutput ([[model]].toJson (null))
	}
	
}
package [[package]].[[models]]

import java.util.List

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
 * 				 response.set ('X-MyHeader', 'Hello')
 *				 you can also write data to the response but this is rarely will happen as the platform takes care of this.	
 *
 *	//@author	[[user]]
 *	//@created	[[date]]
 * 
 **/

@SerialVersionUID ([[randLong]]L)
class Add[[Model]][[Ref]] extends AbstractApiServiceSpi {

	@throws (classOf [ApiServiceExecutionException])
	def execute (api: Api, consumer: ApiConsumer, request: ApiRequest, response: ApiResponse): ApiOutput = {
		
		// Add a [[Ref]] to [[Model]]
		
		val [[model]]Id: Object = request.get ("[[model]]")
		val [[ref]]Id: Object 	= request.get ("[[ref]]")
		
		// write to database
		val db: Database = feature (api, classOf[Database], null, request).trx ()
		
		var [[model]]: DatabaseObject = null
		try {
			// lookup [[Model]] by :[[model]]
			[[model]] = db.get ("[[Model]]", [[model]]Id)
		} catch {
			case dbex: DatabaseException => throw new ApiServiceExecutionException (dbex.getMessage, dbex)
		}	
		if ([[model]] == null) {
			throw new ApiServiceExecutionException (
				api.message (request.getLang, "NotFound", "[[model]]", [[model]]Id)
			).status (ApiResponse.NOT_FOUND)
		}
		
		var [[ref]]: DatabaseObject = null
		try {
			// lookup [[Ref]] by :[[ref]]
			[[ref]] = db.get ("[[RefSpec.entity]]", [[ref]]Id)
		} catch {
			case dex: DatabaseException => throw new ApiServiceExecutionException (dbex.getMessage, dbex)
		}	
		if ([[ref]] == null) {
			throw new ApiServiceExecutionException (
				api.message (request.getLang, "NotFound", "[[ref]]", [[ref]]Id)
			).status (ApiResponse.NOT_FOUND)
		}
		
		@SuppressWarnings("unchecked")
		var [[refs]]: List<DatabaseObject> = [[model]].get ("[[refs]]").asInstanceOf[List<DatabaseObject>]
		
		if ([[refs]] == null) {
			try {
				[[refs]] = db.createList ()
				[[model]].set ("[[refs]]", [[refs]])
			} catch {
				case dbex: DatabaseException => throw new ApiServiceExecutionException (dbex.getMessage, dbex)
			}
		}

		// add [[ref]] to [[Model]].[[refs]]
		[[refs]].add ([[ref]])
		
		try {
			// save [[model]]
			[[model]].save ()
		} catch {
			case dbex: DatabaseException => throw new ApiServiceExecutionException (dbex.getMessage, dbex)
		}
		
		new JsonApiOutput ([[model]].toJson (null))
	}
	
}
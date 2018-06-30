package [[package]]

import com.bluenimble.platform.api.Api
import com.bluenimble.platform.api.ApiRequest
import com.bluenimble.platform.api.ApiService
import com.bluenimble.platform.api.impls.spis.AbstractApiSpi

import com.bluenimble.platform.api.security.ApiAuthenticationException
import com.bluenimble.platform.api.security.ApiConsumer

@SerialVersionUID ([[randLong]]L)
class [[Api]]Api extends AbstractApiSpi {

	@throws (classOf [ApiAuthenticationException])
	def findConsumer (api: Api, service: ApiService, request: ApiRequest, consumer: ApiConsumer): Unit = {
		/**
		 *
		 * Remove this portion of code if you want to write your own authentication logic.
		 * This code remains valid only if you're using BlueNimble default authentication schemes see api.json / security section
		 *
		 **/

		/**
		 *
		 * Authorize call if it's originating from another service in the same space 
		 *
		 **/
		if (ApiRequest.Channels.container.name == request.getChannel) {
			consumer.`override` (
				request.get (ApiRequest.Consumer).asInstanceOf[ApiConsumer]
			)
            return
        }
		
        if (!isSecure (service)) {
			return
		}
		
	    if (ApiConsumer.Type.Unknown == consumer.`type` && consumer.isAnonymous) {
			throw new ApiAuthenticationException ("authentication error")
		}

	}

}
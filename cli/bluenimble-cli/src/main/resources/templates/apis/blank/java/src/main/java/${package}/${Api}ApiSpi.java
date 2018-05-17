package ${package};

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.impls.spis.AbstractApiSpi;

import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.security.ApiConsumer.Type;

public class ${Api}ApiSpi extends AbstractApiSpi {

	private static final long serialVersionUID = 8197725424778011778L;

	@Override
	public void findConsumer (Api api, ApiService service, ApiRequest request, ApiConsumer consumer) throws ApiAuthenticationException {
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
		if (ApiRequest.Channels.container.name ().equals (request.getChannel ())) {
			consumer.override (
				(ApiConsumer)request.get (ApiRequest.Consumer)
			);
            return;
        }
		
        if (!this.isSecure (service) ) {
			return;
		}
		
		Type type 	= consumer.type ();
		
	    if (ApiConsumer.Type.Unknown.equals (consumer.type ()) && consumer.isAnonymous () && isSecure (service) ) {
			throw new ApiAuthenticationException ("authentication error");
		}

	}

}
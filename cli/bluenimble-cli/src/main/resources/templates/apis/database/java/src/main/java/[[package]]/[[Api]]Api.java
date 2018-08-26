package [[package]];

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.impls.spis.AbstractApiSpi;

import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;

public class [[Api]]Api extends AbstractApiSpi {

	private static final long serialVersionUID = [[randLong]]L;

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
		
        if (!isSecure (service) ) {
			return;
		}
		
	    if (ApiConsumer.Type.Unknown.equals (consumer.type ()) && consumer.isAnonymous ()) {
			throw new ApiAuthenticationException ("authentication error");
		}

	}

	@Override
	public void	onExecute	(Api api, ApiConsumer consumer, ApiService service, ApiRequest request, ApiResponse response) {
		if (consumer.get (ApiConsumer.Fields.Id) == null) {
            return;
        }

        if (consumer.get (ApiConsumer.Fields.Owner) == null) {
			consumer.set (ApiConsumer.Fields.Owner, consumer.get (ApiConsumer.Fields.Id));
		}
	}

}
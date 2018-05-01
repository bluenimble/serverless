package com.bluenimble.platform.api.impls.proxy;

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;

public class BinaryProxyApiServiceSpi implements ApiServiceSpi {

	private static final long serialVersionUID = -7448782202115381461L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {

		// acquire a binary client from the space
		
		//factory api.space ().getRecyclable, the get the client
		
		return null;
	}

	@Override
	public void onStart (Api api, ApiService service, ApiContext context) throws ApiManagementException {
		
	}

	@Override
	public void onStop (Api api, ApiService service, ApiContext context) throws ApiManagementException {
		
	}

}

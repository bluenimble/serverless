package com.bluenimble.platform.server.plugins.inbound.binary.netty;

import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;

public interface NettyRequestProcessor {

	ApiResponse process (ApiRequest request) throws Exception;
	
}

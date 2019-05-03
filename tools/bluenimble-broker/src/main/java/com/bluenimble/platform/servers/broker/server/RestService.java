package com.bluenimble.platform.servers.broker.server;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface RestService {

	boolean 			isSecure ();
	FullHttpResponse 	execute (Broker broker, FullHttpRequest request);
	
}

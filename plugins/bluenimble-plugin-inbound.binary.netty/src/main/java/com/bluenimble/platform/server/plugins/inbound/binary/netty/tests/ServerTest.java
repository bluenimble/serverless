package com.bluenimble.platform.server.plugins.inbound.binary.netty.tests;

import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.server.plugins.inbound.binary.netty.NettyRequestProcessor;
import com.bluenimble.platform.server.plugins.inbound.binary.netty.NettyResponse;
import com.bluenimble.platform.server.plugins.inbound.binary.netty.NettyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;

public class ServerTest {

	public static void main (String [] args) throws Exception {
		NettyServer server = new NettyServer (7070, new NettyRequestProcessor () {
			@Override
			public ApiResponse process (ApiRequest request) throws Exception {
		    	System.out.println (request.toJson ().toString (2, true));
				ApiResponse response = 
					new NettyResponse (request.getId (), request.getNode (), (ChannelHandlerContext)request.get (NettyServer.Context));
				
				response.setStatus (ApiResponse.CREATED);
				
				// add some headers
				response.set ("h1", "v1");
				response.set ("h2", 100);
				response.set ("h3", true);
				
				response.write ("Hello".getBytes ());
				response.write (" Simo".getBytes ());
				
				response.close ();
				
				return response;
			}
		});
		server.setLogLevel (LogLevel.INFO);
		server.start ();
	}
	
}

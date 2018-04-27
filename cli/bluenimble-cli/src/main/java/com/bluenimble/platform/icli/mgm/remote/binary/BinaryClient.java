/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.icli.mgm.remote.binary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.Null;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public final class BinaryClient {
	
	private EventLoopGroup 			group;
	
	private ChannelHandlerContext 	context;

	private String 					host; 
	private int 					port;
	
	private boolean					disconnect;
	
	Map<String, ResponseCallback> callbacks = new ConcurrentHashMap<String, ResponseCallback> ();
	
	public BinaryClient (String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public void connect () throws Exception {
		// Configure SSL.
		//final SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build ();

		group = new NioEventLoopGroup ();
		Bootstrap b = new Bootstrap ();
		b.group (group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				/*if (sslCtx != null) {
					p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
				}*/
				p.addLast (
					new ObjectEncoder (), 
					new ObjectDecoder (ClassResolvers.cacheDisabled (null)),
					new BinaryClientHandler ()
				);
			}
		});

		b.connect (host, port).sync ();
	}
	
	public void disconnect () throws InterruptedException {
		if (disconnect) {
            group.shutdownGracefully ();
            group = null;
        }
		this.disconnect = true;
	}
	
	public void send (ApiRequest request, ResponseCallback callback) {
		if (disconnect) {
            // exception
        }
		callbacks.put (request.getId (), callback);
		context.writeAndFlush (request);
	} 
	
	class BinaryClientHandler extends ChannelInboundHandlerAdapter {
		
		private ResponseCallback callback = null;

		@Override
	    public void channelActive (ChannelHandlerContext context) {
			BinaryClient.this.context = context;
	    }

	    @SuppressWarnings("unchecked")
		@Override
	    public void channelRead (ChannelHandlerContext ctx, Object msg) {
	    	//System.out.println (callback);
	    	if (msg instanceof String) {
	    		String requestId = (String)msg;
	    		callback = callbacks.get (requestId);
	    	} else if (msg instanceof ApiResponse.Status) {
		    	callback.onStatus ((ApiResponse.Status)msg);
	    	} else if (msg instanceof Map) {
		    	callback.onHeaders ((Map<String, Object>)msg);
	    	} else if (msg instanceof byte []) {
		    	callback.onChunk ((byte [])msg);
	    	} else if (msg instanceof Null) {
	    		callback.onFinish ();
	    		ctx.flush ();
	    	}
	    }

	    @Override
	    public void channelReadComplete (ChannelHandlerContext ctx) {
	    	/*
	    	ResponseCallback callback = context.channel().attr (Callback).get ();
	    	System.out.println (callback);
	    	callback.onFinish ();
	        */
	    	if (disconnect) {
	            group.shutdownGracefully ();
	            group = null;
	        }
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	        cause.printStackTrace();
	        ctx.close();
	    }

	}
}
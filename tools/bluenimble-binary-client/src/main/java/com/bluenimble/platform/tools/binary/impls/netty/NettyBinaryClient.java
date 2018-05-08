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
package com.bluenimble.platform.tools.binary.impls.netty;

import java.io.IOException;
import java.util.Map;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.tools.binary.BinaryClient;
import com.bluenimble.platform.tools.binary.BinaryClientCallback;
import com.bluenimble.platform.tools.binary.BinaryClientException;

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

public class NettyBinaryClient implements BinaryClient {

	private static final long serialVersionUID = -7191583878115216336L;

	private ChannelHandlerContext 	context;
	private DefaultHandler			handler;

	private EventLoopGroup 			group;
	
	public NettyBinaryClient (String host, int port) throws BinaryClientException {
		
		handler = new DefaultHandler ();
		
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
					handler
				);
			}
		});

		try {
			b.connect (host, port).sync ();
		} catch (InterruptedException e) {
			throw new BinaryClientException (e.getMessage (), e);
		}
	}

	@Override
	public void send (JsonObject request, BinaryClientCallback callback) {
		handler.callback = callback;
		context.writeAndFlush (request.toString ().getBytes ());
	}

	@Override
	public void recycle () {
		handler.callback = null;
	}
	
	public void destroy () {
		group.shutdownGracefully ();
        group = null;
	}
	
	class DefaultHandler extends ChannelInboundHandlerAdapter {
		
		BinaryClientCallback callback = null;

		@Override
	    public void channelActive (ChannelHandlerContext context) {
			NettyBinaryClient.this.context = context;
	    }

	    @SuppressWarnings("unchecked")
		@Override
	    public void channelRead (ChannelHandlerContext ctx, Object msg) {
	    	if (callback == null) {
	    		return;
	    	}
	    	try {
		    	if (msg instanceof Integer) {
			    	callback.onStatus ((Integer)msg);
		    	} else if (msg instanceof Map) {
			    	callback.onHeaders ((Map<String, Object>)msg);
		    	} else if (msg instanceof byte []) {
					callback.onChunk ((byte [])msg);
		    	} 
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
	    }

	    @Override
	    public void channelReadComplete (ChannelHandlerContext ctx) {
	    	if (callback == null) {
	    		return;
	    	}
	    	try {
	    		callback.onFinish ();
			} catch (IOException e) {
				throw new RuntimeException (e.getMessage (), e);
			}
    		ctx.flush ();
    		callback = null;
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	        cause.printStackTrace();
	        ctx.close ();
	    }

	}

}

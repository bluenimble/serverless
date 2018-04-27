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
package com.bluenimble.platform.server.plugins.inbound.binary.netty;

import com.bluenimble.platform.api.ApiRequest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {
	
	private NettyRequestProcessor proessor;
	
	public NettyServerHandler (NettyRequestProcessor proessor) {
		this.proessor = proessor;
	}

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) {
    	ApiRequest request = (ApiRequest)msg;
    	request.set (NettyServer.Context, ctx);
    	
    	ctx.write (request.getId ());
    	try {
			proessor.process (request);
		} catch (Exception e) {
			throw new RuntimeException (e.getMessage (), e);
		}
    }

    @Override
    public void channelReadComplete (ChannelHandlerContext ctx) {
        ctx.flush ();
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
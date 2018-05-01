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

import java.net.InetSocketAddress;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.impls.SimpleApiRequest;
import com.bluenimble.platform.json.JsonObject;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {
	
	private static final String Device = ApiRequest.Fields.Device.class.getSimpleName ().toLowerCase ();
	
	private NettyRequestProcessor proessor;
	
	public NettyServerHandler (NettyRequestProcessor proessor) {
		this.proessor = proessor;
	}

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) {
    	try {
    		JsonObject data = new JsonObject (new String ((byte [])msg));
    		
    		// set bnb channel
    		data.set (ApiRequest.Fields.Channel, NettyServer.Channel);
    		
    		// set / create device
    		JsonObject device = Json.getObject (data, Device);
    		if (device == null) {
    			device = new JsonObject ();
    			data.set (Device, device);
    		}
    		
    		device.set (ApiRequest.Fields.Device.Origin, ((InetSocketAddress )ctx.channel().remoteAddress ()).getAddress ().getHostAddress ());
    		
    		// set origin, agent and channel 
    		
        	ApiRequest request = new SimpleApiRequest (data);
        	request.set (NettyServer.Context, ctx);
        	
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
        ctx.close ();
    }
}
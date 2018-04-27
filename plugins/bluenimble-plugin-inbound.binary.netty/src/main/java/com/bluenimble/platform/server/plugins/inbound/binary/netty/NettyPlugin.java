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
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.CodeExecutor;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;

public class NettyPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;

	private int 		port = 7070;
	private JsonObject	options;
	
	@SuppressWarnings("unchecked")
	@Override
	public void init (final ApiServer server) throws Exception {

		// init
		
		NettyServer nettyServer = new NettyServer (port, new NettyRequestProcessor () {
			@Override
			public ApiResponse process (ApiRequest request) throws Exception {
				ApiResponse response = new NettyResponse (request.getId (), (ChannelHandlerContext)request.get (NettyServer.Context));
				
				request.getNode ().set (ApiRequest.Fields.Node.Id, server.id ());
    			request.getNode ().set (ApiRequest.Fields.Node.Type, server.type ());
    			request.getNode ().set (ApiRequest.Fields.Node.Version, server.version ());
    			
    			server.execute (request, response, CodeExecutor.Mode.Sync);
    			
				return response;
			}
			
		});
		
		if (options != null) {
			nettyServer.setOptions (options);
		}
		
		nettyServer.setLogLevel (LogLevel.INFO);
		nettyServer.start ();

	}

	public int getPort () {
		return port;
	}
	public void setPort (int port) {
		this.port = port;
	}
	
	public JsonObject getOptions () {
		return options;
	}
	public void setOptions (JsonObject options) {
		this.options = options;
	}
	
}

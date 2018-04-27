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

import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.security.SslUtils;
import com.bluenimble.platform.security.SslUtils.StoreSource;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public final class NettyServer {
	
	public static final String Context = "Netty.Context";
	
	private EventLoopGroup bossGroup;;
	private EventLoopGroup workerGroup;

	protected int 			port;
	protected boolean 		selfSignedSsl;
	
	protected StoreSource 	keystore;
	protected StoreSource 	truststore;
	
	protected int			maxPayloadSize = 20*1024*1024;
	
	protected LogLevel		logLevel;
	
	protected Map<String, Object>		
							options;
	
	protected NettyRequestProcessor 
							processor;
	
	public NettyServer (int port, NettyRequestProcessor processor) {
		this.port 		= port;
		this.processor 	= processor;
	}
	
	public NettyServer () {
	}
	
	@SuppressWarnings("unchecked")
	public void start () throws Exception {
		
		final SslContext sslCtx = createSslContext ();
		
		bossGroup = new NioEventLoopGroup (1);
		workerGroup = new NioEventLoopGroup ();
		
		try {
			ServerBootstrap b = new ServerBootstrap ()
				.group (bossGroup, workerGroup).channel (NioServerSocketChannel.class);
			
			if (options != null && !options.isEmpty ()) {
				Set<String> keys = options.keySet ();
				for (String key : keys) {
					b.option ((ChannelOption<Object>)ChannelOption.class.getField (key.toUpperCase ()).get (null), options.get (key));
				}
			}
			
			if (logLevel != null) {
				b.handler (new LoggingHandler (logLevel));
			}
					
			b.childHandler (new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
					if (sslCtx != null) {
						p.addLast (sslCtx.newHandler(ch.alloc()));
					}
					p.addLast (
						new ObjectEncoder (), 
						new ObjectDecoder (maxPayloadSize, ClassResolvers.cacheDisabled (null)),
						new NettyServerHandler (processor)
					);
				}
			});

			// Bind and start to accept incoming connections.
			b.bind (port).sync ().channel ().closeFuture ().sync ();
		} finally {
			bossGroup.shutdownGracefully ();
			workerGroup.shutdownGracefully ();
		}
		
	}
	
	public void stop () {
		bossGroup.shutdownGracefully ();
		workerGroup.shutdownGracefully ();
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public StoreSource getKeystore() {
		return keystore;
	}

	public void setKeystore(StoreSource keystore) {
		this.keystore = keystore;
	}

	public StoreSource getTruststore() {
		return truststore;
	}
	public void setTruststore(StoreSource truststore) {
		this.truststore = truststore;
	}

	public LogLevel getLogLevel() {
		return logLevel;
	}
	public void setLogLevel (LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	public int getMaxPayloadSize() {
		return maxPayloadSize;
	}
	public void setMaxPayloadSize (int maxPayloadSize) {
		this.maxPayloadSize = maxPayloadSize;
	}

	public Map<String, Object> getOptions () {
		return options;
	}
	public void setOptions (Map<String, Object> options) {
		this.options = options;
	}

	private SslContext createSslContext () throws Exception {
		SslContext sslCtx = null;
		if (keystore != null || truststore != null) {
			sslCtx = new JdkSslContext (SslUtils.sslContext (keystore, truststore), false, null);
		} else if (selfSignedSsl) {
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContextBuilder.forServer (ssc.certificate (), ssc.privateKey ()).build ();
		}
		return sslCtx;
	}
	
}
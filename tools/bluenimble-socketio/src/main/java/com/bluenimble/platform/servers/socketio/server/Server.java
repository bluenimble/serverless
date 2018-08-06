package com.bluenimble.platform.servers.socketio.server;

import java.io.Serializable;

public interface Server extends Serializable {

	enum Events {
		join,
		leave,
		publish,
		message,
		error
	}
	
	interface Spec {
		String Host 					= "host";
		String Port 					= "port";
		String AddVersion				= "addVersion";
		String AllowCustomRequests		= "allowCustomRequests";
		
		String BossThreads				= "bossThreads";
		String WorkThreads				= "workThreads";
		
		String Context					= "context";
		
		// timeouts
		String FirstDataTimeout			= "firstDataTimeout";
		
		String Compression				= "compression";
		
		String AllowOrigin				= "allowOrigin";
		
		String MaxFramePayloadLength	= "maxFramePayloadLength";
		String MaxHttpContentLength		= "maxHttpContentLength";
		
		String PingInterval				= "pingInterval";
		String PingTimeout				= "pingTimeout";
		String UpgradeTimeout			= "upgradeTimeout";
		
		String SSLProtocol				= "sslProtocol";
		
		String UseNativeEpoll			= "useNativeEpoll";
		
		interface Ssl {
			String Protocol				= "Protocol";	 
			String Store				= "store";
			String Format				= "format";
			String Password				= "password";
		}
		
		interface Auth {
			String Class				= "class";
		}
		
		
	}

	void start 	();
	void stop 	();
	
}

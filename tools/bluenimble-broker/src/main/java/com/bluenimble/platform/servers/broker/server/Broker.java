package com.bluenimble.platform.servers.broker.server;

import java.io.Serializable;
import java.util.Date;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.TenantProvider;
import com.bluenimble.platform.servers.broker.security.SelectiveAuthorizationListener;
import com.corundumstudio.socketio.SocketIOServer;

public interface Broker extends Serializable {

	interface Spec {
		String Host 					= "host";
		String Port 					= "port";
		String AddVersion				= "addVersion";
		
		String ReuseAddress				= "reuseAddress";
		
		String AddShutdownHook			= "addShutdownHook";
		
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
		
		String UseNativeEpoll			= "useNativeEpoll";
		
		interface Ssl {
			String Protocol				= "protocol";	 
			String Store				= "store";
			String Format				= "format";
			String Password				= "password";
		}
		
		String TenantProvider  			= "tenantProvider";
		String Auths  					= "auths";
		String Listeners  				= "listeners";
		String AccessibleBy  			= "accessibleBy";
		String ApiPath					= "apiPath";	
		String PingPath					= "pingPath";	
		
		interface Store {
			String Config				= "config";
		}
		
		interface Api {
			String Path					= "path";
			String Ping					= "ping";
			String Auth					= "auth";
				String Tokens			= "tokens";
		}
		
	}
	
	TenantProvider 	getTenantProvider ();
	SelectiveAuthorizationListener
					getAuthorizationListener ();
	
	SocketIOServer	server 		();

	void 			start 		() throws BrokerException;
	void 			stop 		();
	
	String			id 			();
	Date			startTime 	();
	JsonObject		describe 	();
	
}

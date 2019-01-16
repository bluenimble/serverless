package com.bluenimble.platform.servers.broker.server;

import java.io.Serializable;

import com.bluenimble.platform.servers.broker.TenantProvider;

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
		
		interface Store {
			String Config				= "config";
		}
		
	}
	
	TenantProvider 	getTenantProvider ();

	void 			start 	() throws BrokerException;
	void 			stop 	();
	
}

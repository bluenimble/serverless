package com.bluenimble.platform.servers.broker.server.impls;

import java.io.File;
import java.io.FileInputStream;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.listeners.OnConnectListener;
import com.bluenimble.platform.servers.broker.listeners.OnJoinListener;
import com.bluenimble.platform.servers.broker.listeners.OnLeaveListener;
import com.bluenimble.platform.servers.broker.listeners.OnPublishListener;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;

public class BrokerImpl implements Broker {

	private static final long serialVersionUID = 5738360161781372247L;
	
	private Configuration 	config;
	private SocketIOServer 	server;
	
	public BrokerImpl (JsonObject spec) throws Exception {
		
		config = new Configuration ();
		config.setHostname (Json.getString (spec, Spec.Host));
		config.setPort (Json.getInteger (spec, Spec.Port, 10443));
		config.setAddVersionHeader (Json.getBoolean (spec, Spec.AddVersion, true));
		config.setContext (Json.getString (spec, Spec.Context, "/socket.io"));
		config.setOrigin (Json.getString (spec, Spec.AllowOrigin));
		
		config.setBossThreads (Json.getInteger (spec, Spec.BossThreads, 0));
		config.setWorkerThreads (Json.getInteger (spec, Spec.WorkThreads, 0));
		config.setUseLinuxNativeEpoll (Json.getBoolean (spec, Spec.UseNativeEpoll, false));
		
		config.setAllowCustomRequests (Json.getBoolean (spec, Spec.AllowCustomRequests, false));
		
		config.setHttpCompression (Json.getBoolean (spec, Spec.Compression, true));
		config.setWebsocketCompression (Json.getBoolean (spec, Spec.Compression, true));

		config.setMaxFramePayloadLength (Json.getInteger (spec, Spec.MaxFramePayloadLength, 64) * 1024); // in kb
		config.setMaxHttpContentLength (Json.getInteger (spec, Spec.MaxHttpContentLength, 64) * 1024); // in kb
		
		config.setUpgradeTimeout (Json.getInteger (spec, Spec.UpgradeTimeout, 10) * 1000); // in secs
		config.setFirstDataTimeout (Json.getInteger (spec, Spec.FirstDataTimeout, 5000));
		config.setPingInterval (Json.getInteger (spec, Spec.PingInterval, 25) * 1000); // in secs
		config.setPingTimeout (Json.getInteger (spec, Spec.PingTimeout, 60) * 1000); // in secs
		
		JsonObject ssl = Json.getObject (spec, Spec.Ssl.class.getSimpleName ().toLowerCase ());
		if (!Json.isNullOrEmpty (ssl)) {
			config.setSSLProtocol (Json.getString (ssl, Spec.Ssl.Protocol, "TLSv1"));
			config.setKeyStore (new FileInputStream (new File (Json.getString (ssl, Spec.Ssl.Store))));
			config.setKeyStoreFormat (Json.getString (ssl, Spec.Ssl.Format, "JKS"));
			config.setKeyStorePassword (Json.getString (ssl, Spec.Ssl.Password));
		}
		
		// socket config
		SocketConfig sockConfig = new SocketConfig ();
		sockConfig.setReuseAddress (Json.getBoolean (spec, Spec.ReuseAddress, true));
		config.setSocketConfig (sockConfig);
		
		// shutdown 
		if (Json.getBoolean (spec, Spec.AddShutdownHook, false)) {
			Runtime.getRuntime ().addShutdownHook (new Thread (server::stop));
		}
		
		if (!spec.containsKey (Spec.Auth.class.getSimpleName ().toLowerCase ())) {
			return;
		}
		
		JsonObject oAuth = Json.getObject (spec, Spec.Auth.class.getSimpleName ().toLowerCase ());
		if (Json.isNullOrEmpty (oAuth)) {
			return;
		}
		
		String className = Json.getString (oAuth, Spec.Auth.Class);
		
		AuthorizationListener authListener = (AuthorizationListener)Broker.class.getClassLoader ().loadClass (className)
			.getConstructor (new Class [] { JsonObject.class }).newInstance (new Object [] { oAuth });
		
		config.setAuthorizationListener (authListener);
		
	}
	
	@Override
	public void start () {

		server = new SocketIOServer (config);
		
		server.addEventListener (Events.join.name (), JsonObject.class, new OnJoinListener (server));
		server.addEventListener (Events.leave.name (), JsonObject.class, new OnLeaveListener (server));
		server.addEventListener (Events.publish.name (), JsonObject.class, new OnPublishListener (server));

		// timeout to send auth event
		server.addConnectListener (new OnConnectListener (server));

		server.start ();
		
	}

	@Override
	public void stop () {
		server.stop ();
	}
	
}

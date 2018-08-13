package com.bluenimble.platform.servers.broker.server.impls;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.BeanUtils;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.security.SelectiveAuthorizationListener;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.bluenimble.platform.servers.broker.server.BrokerException;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;

public class BrokerImpl implements Broker {

	private static final long serialVersionUID = 5738360161781372247L;
	
	private static final Logger logger = LoggerFactory.getLogger (BrokerImpl.class);
	
	private SelectiveAuthorizationListener 	authListener;
	
	private JsonObject 						spec;
	
	private Configuration 					config;
	private SocketIOServer 					server;
	
	public BrokerImpl (JsonObject spec) throws Exception {
		
		this.spec = spec;
		
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
			config.setSSLProtocol (Json.getString (ssl, Spec.Ssl.Protocol, "TLSv12"));
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
		
		authListener = new SelectiveAuthorizationListener (config.getContext ());
		
		config.setAuthorizationListener (authListener);
		
	}
	
	@Override
	public void start () throws BrokerException {

		server = new SocketIOServer (config);
		
		// load namespaces
		try {
			load ();
		} catch (Exception ex) {
			throw new BrokerException (ex.getMessage (), ex);
		}

		server.start ();
		
	}

	@Override
	public void stop () {
		server.stop ();
	}
	
	private void load () throws Exception {
		
		JsonObject namespaces 	= Json.getObject (spec, Spec.Namespaces);
		JsonObject auths 		= Json.getObject (spec, Spec.Auths);
		
		if (Json.isNullOrEmpty (namespaces)) {
			return;
		}
		
		logger.info ("Load Namespaces");
		
		Map<String, AuthorizationListener> authListeners = loadAuths (auths);
		
		Iterator<String> nsKeys = namespaces.keys ();
		while (nsKeys.hasNext ()) {
			String name = nsKeys.next ();
			
			JsonObject oNs = Json.getObject (namespaces, name);
			
			loadNamespace (name, oNs);
			
			JsonArray nsAuths = Json.getArray (oNs, Spec.Auths);
			if (Json.isNullOrEmpty (nsAuths)) {
				continue;
			}
			for (int i = 0; i < nsAuths.count (); i++) {
				authListener.addListener (name, authListeners.get (nsAuths.get (i)));
			}
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadNamespace (String name, JsonObject oNs) throws Exception {
		
		logger.info ("Load Namespace: " + name);
		
		JsonObject oListeners = Json.getObject (oNs, Spec.Listeners);
		if (Json.isNullOrEmpty (oListeners)) {
			return;
		}
		
		if (!name.startsWith (Lang.SLASH)) {
			name = Lang.SLASH + name;
		}
		
		SocketIONamespace namespace = null;
		
		boolean isGlobal = name.equals (Lang.SLASH);
		
		if (!isGlobal) {
			namespace = server.addNamespace (name);
		}
		
		logger.info ("\tNamespace Instance: " + namespace);
		
		// add connect/disconnect listeners
		if (isGlobal) {
			server.addConnectListener (new OnConnectListener (server, null));
			server.addDisconnectListener (new OnDisconnectListener (server, null));
		} else {
			namespace.addConnectListener (new OnConnectListener (null, namespace));
			namespace.addDisconnectListener (new OnDisconnectListener (null, namespace));
		}
		
		Iterator<String> lstKeys = oListeners.keys ();
		while (lstKeys.hasNext ()) {
			String event = lstKeys.next ();
			
			logger.info ("Found Event: " + event);

			JsonObject oListener = Json.getObject (oListeners, event);
			
			Set<String> accessibleBy = null;
			JsonArray aAccessibleBy = Json.getArray (oListener, Spec.AccessibleBy);
			if (!Json.isNullOrEmpty (aAccessibleBy)) {
				accessibleBy = new HashSet<String> ();
				for (Object o : aAccessibleBy) {
					accessibleBy.add (String.valueOf (o));
				}
			}
			
			EventListener listener = (EventListener)BeanUtils.create (oListener);
			
			logger.info ("\tEvent Listener: " + listener);
			
			Class<?> targetCls = isGlobal ? SocketIOServer.class : SocketIONamespace.class;

			targetCls.getMethod (
				"addEventListener", 
				new Class [] { String.class, Class.class, DataListener.class }
			).invoke (
				isGlobal ? server : namespace, 
				new Object [] { 
					event, 
					listener.dataType (), 
					new DelegateListener (listener, accessibleBy)
				}
			);
			logger.info ("\tListener " + event + " added to namespace: " + name);
			
		}
		
	}
	
	private Map<String, AuthorizationListener> loadAuths (JsonObject auths) throws Exception {
		
		Map<String, AuthorizationListener> listeners = new HashMap<String, AuthorizationListener> ();
		
		if (Json.isNullOrEmpty (auths)) {
			return listeners;
		}
		
		Iterator<String> authKeys = auths.keys ();
		while (authKeys.hasNext ()) {
			String auth = authKeys.next ();
			JsonObject oAuth = auths.getObject (auth);
			
			listeners.put (auth, (AuthorizationListener)BeanUtils.create (oAuth));
		}
		
		return listeners;
		
	}
	
}

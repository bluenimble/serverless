package com.bluenimble.platform.servers.broker.server.impls;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.BeanUtils;
import com.bluenimble.platform.servers.broker.TenantProvider;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.security.SelectiveAuthorizationListener;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.bluenimble.platform.servers.broker.server.BrokerException;
import com.bluenimble.platform.servers.broker.server.RestService;
import com.bluenimble.platform.servers.broker.server.impls.api.BroadcastRestService;
import com.bluenimble.platform.servers.broker.server.impls.api.PingRestService;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.api.RestApi;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.store.RedissonStoreFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

public class BrokerImpl implements Broker {

	private static final long serialVersionUID = 5738360161781372247L;
	
	private static final Logger logger = LoggerFactory.getLogger (BrokerImpl.class);
	
	private static final RestService PingService = new PingRestService ();
	
	private SelectiveAuthorizationListener 	authListener;
	private TenantProvider					tenantProvider;
	
	private JsonObject 						spec;
	
	private Configuration 					config;
	private SocketIOServer 					server;
	
	private File 							home;
	
	private String							id;
	private Date							startTime;
	private JsonObject						describe;
	
	private static final Map<String, RestService> 		
											Services = new HashMap<String, RestService>();
	static {
		Services.put ("/_info", PingService);
		Services.put ("POST/api/messages", new BroadcastRestService ());
	}
	
	public BrokerImpl (File home) throws Exception {
		
		this.home = home;
		this.spec = Json.load (new File (home, "broker.json"));
		
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

		config.setMaxFramePayloadLength (Json.getInteger (spec, Spec.MaxFramePayloadLength, 1) * 1024 * 1024); // in kb
		config.setMaxHttpContentLength (Json.getInteger (spec, Spec.MaxHttpContentLength, 1) * 1024 * 1024); // in kb
		
		config.setUpgradeTimeout (Json.getInteger (spec, Spec.UpgradeTimeout, 10) * 1000); // in secs
		config.setFirstDataTimeout (Json.getInteger (spec, Spec.FirstDataTimeout, 5000));
		config.setPingInterval (Json.getInteger (spec, Spec.PingInterval, 25) * 1000); // in secs
		config.setPingTimeout (Json.getInteger (spec, Spec.PingTimeout, 60) * 1000); // in secs
		
		// Cluster Store
		JsonObject oStore = Json.getObject (spec, Spec.Store.class.getSimpleName ().toLowerCase ());
		if (!Json.isNullOrEmpty (oStore)) {
			Config rConfig = Config.fromJSON (new File (home, Json.getString (oStore, Spec.Store.Config)));
			RedissonClient client = Redisson.create (rConfig);
			
			// Instantiate RedissonClientStoreFactory
			RedissonStoreFactory redisStoreFactory = new RedissonStoreFactory (client);
			config.setStoreFactory (redisStoreFactory);
		}
		
		// SSL
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
		
		authListener = new SelectiveAuthorizationListener (this, server);
		
		config.setAuthorizationListener (authListener);
		
		JsonObject oApi 	= Json.getObject (spec, Spec.Api.class.getSimpleName ().toLowerCase ());
		String pingPath 	= Json.getString (oApi, Spec.Api.Ping, "/_info");
		String apiPath 		= Json.getString (oApi, Spec.Api.Path, "/api");
		JsonObject tokens 	= Json.getObject (oApi, Spec.Api.Tokens);
		
		if (oApi != null) {
			config.setRestApi (new RestApi () {
				@Override
				public FullHttpResponse	process (FullHttpRequest request) {
					logger.info ("Process Api Request " + request.uri ());
					QueryStringDecoder queryDecoder = new QueryStringDecoder (request.uri ());
					String path = queryDecoder.path ();
					if (!path.equals (pingPath) && !path.startsWith (apiPath)) {
						return null;
					}
					if (path.equals (pingPath)) {
						return PingService.execute (BrokerImpl.this, request);
					}
					logger.info ("Servive ID " + request.method ().name () + path);
					RestService service = Services.get (request.method ().name () + path);
					if (service == null) {
						ByteBuf content = Unpooled.copiedBuffer ("Service " + request.method ().name () + path + " not available", CharsetUtil.UTF_8);
						return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.NOT_FOUND, content);
					}
					if (service.isSecure ()) {
						String authHeader = request.headers ().get (ApiHeaders.Authorization);
						if (Lang.isNullOrEmpty (authHeader)) {
							ByteBuf content = Unpooled.copiedBuffer ("Token Unavailable", CharsetUtil.UTF_8);
							return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, content);
						}
						String [] authEntries = Lang.split (authHeader, Lang.SPACE, true);
						if (authEntries.length < 2) {
							ByteBuf content = Unpooled.copiedBuffer ("Invalid Authorization Header", CharsetUtil.UTF_8);
							return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, content);
						}
						if (!tokens.containsKey (authEntries [1])) {
							ByteBuf content = Unpooled.copiedBuffer ("Invalid Authorization Token", CharsetUtil.UTF_8);
							return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, content);
						}
					}
					logger.info ("Execute Service " + service.getClass ().getSimpleName ());
					return service.execute (BrokerImpl.this, request);
				}
			});
		}

		
	}
	
	@Override
	public void start () throws BrokerException {
		
		id = Lang.UUID (10);
		
		startTime = new Date ();
		
		describe = new JsonObject ();
		describe.set ("id", id);
		describe.set ("sts", Lang.toUTC (startTime));

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
	
	@Override
	public TenantProvider getTenantProvider () {
		return tenantProvider;
	}
	
	private void load () throws Exception {
		
		tenantProvider = (TenantProvider)BeanUtils.create (Json.getObject (spec, Spec.TenantProvider));
		tenantProvider.init (this, home);
		
		JsonObject listeners 	= Json.getObject (spec, Spec.Listeners);
		JsonObject auths 		= Json.getObject (spec, Spec.Auths);
		
		if (Json.isNullOrEmpty (listeners)) {
			return;
		}
		
		loadAuths (auths);
		
		loadListeners (listeners);
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadListeners (JsonObject oListeners) throws Exception {
		
		server.addConnectListener (new OnConnectListener (server));
		server.addDisconnectListener (new OnDisconnectListener (server));
		
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
			
			SocketIOServer.class.getMethod (
				"addEventListener", 
				new Class [] { String.class, Class.class, DataListener.class }
			).invoke (
				server, 
				new Object [] { 
					event, 
					listener.dataType (), 
					new DelegateListener (this, event, listener, accessibleBy)
				}
			);
			logger.info ("\tListener " + event + " added");
			
		}
		
	}
	
	private void loadAuths (JsonObject auths) throws Exception {
		
		logger.info ("Load Auth Listeners");
		
		if (Json.isNullOrEmpty (auths)) {
			return;
		}
		
		Iterator<String> authKeys = auths.keys ();
		while (authKeys.hasNext ()) {
			String auth = authKeys.next ();
			JsonObject oAuth = auths.getObject (auth);
			
			authListener.addListener (auth, (AuthorizationListener)BeanUtils.create (oAuth));
		}
		
	}

	@Override
	public SocketIOServer server () {
		return server;
	}

	@Override
	public String id () {
		return id;
	}

	@Override
	public Date startTime() {
		return startTime;
	}

	@Override
	public JsonObject describe () {
		return describe;
	}
	
}

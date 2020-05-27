package com.bluenimble.platform.servers.broker.security;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.Tenant;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

public class SelectiveAuthorizationListener implements AuthorizationListener {
	
	private static final Logger logger = LoggerFactory.getLogger (SelectiveAuthorizationListener.class);
	
	private static final String IoHeader 			= "io";
	public 	static final String TenantParameter 	= "tenant";
	
	protected Map<String, AuthorizationListener> listeners = new LinkedHashMap<String, AuthorizationListener> ();
	
	protected	Broker 			broker;
	protected	SocketIOServer 	server;
	
	public SelectiveAuthorizationListener (Broker broker, SocketIOServer server) {
		this.broker = broker;
		this.server = server;
	}
	
	@Override
	public boolean isAuthorized (HandshakeData data) {
		
		String tenantId = data.getSingleUrlParam (TenantParameter);
		if (Lang.isNullOrEmpty (tenantId)) {
			return false;
		}
		
		logger.info ("Tenant ID " + tenantId);
		
		Tenant tenant = broker.getTenantProvider ().get (tenantId);
		if (tenant == null) {
			logger.info ("No tenant found " + tenantId);
			return false;
		}
		
		if (!tenant.available ()) {
			logger.info ("Tenant " + tenantId + " is not available (blocked)");
			return false;
		}
		
		if (tenant.authListener () == null) {
			logger.info ("No auth listeners defined for tenant " + tenantId);
			return false;
		}
		
		AuthorizationListener listener = listeners.get (tenant.authListener ().toLowerCase ());
		
		if (listener == null) {
			logger.error ("Auth listener " + tenant.authListener () + " not found in broker config.");
			return false;
		}
		
		boolean isAuthorized = listener.isAuthorized (data);
		
		if (isAuthorized) {
			logger.info ("\tauthorized: " + isAuthorized);
			overridePeer (data);
			return true;
		}
		
		logger.info ("\tNot Authorized");
		return false;
	}
	
	public void addListener (String name, AuthorizationListener listener) {
		if (name == null || listener == null) {
			return;
		}
		listeners.put (name, listener);
	}
	
	public JsonObject refreshPeer (Peer peer) {
		Tenant tenant = peer.tenant ();
		if (!tenant.available ()) {
			logger.info ("Tenant " + tenant.id () + " is not available (blocked)");
			return null;
		}
		
		if (tenant.authListener () == null) {
			logger.info ("No auth listeners defined for tenant " + tenant.id ());
			return null;
		}
		
		AuthorizationListener listener = listeners.get (tenant.authListener ().toLowerCase ());
		
		if (listener == null) {
			logger.error ("Auth listener " + tenant.id () + "/" + tenant.authListener () + " not found in broker config.");
			return null;
		}
		
		if (!RefreshableAuthorizationListener.class.isAssignableFrom (listener.getClass ())) {
			logger.info ("Authorization Listener " + tenant.authListener () + " not refreshable");
			return null;
		}
		
		return ((RefreshableAuthorizationListener)listener).refreshPeer (peer);
		
	}
	
	private void overridePeer (HandshakeData data) {
		HttpHeaders headers = data.getHttpHeaders ();
		for (String cookieHeader: headers.getAll(HttpHeaderNames.COOKIE)) {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieHeader);
            for (Cookie cookie : cookies) {
                if (cookie.name ().equals (IoHeader)) {
                    SocketIOClient client = server.getClient (UUID.fromString (cookie.value ()));
                    if (client != null) {
                    	headers.add (IoHeader, UUID.randomUUID ().toString ());
                    }
                }
            }
        }
	}

}

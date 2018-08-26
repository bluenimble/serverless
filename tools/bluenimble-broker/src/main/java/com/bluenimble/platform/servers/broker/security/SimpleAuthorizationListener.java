package com.bluenimble.platform.servers.broker.security;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.utils.PeerUtils;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;

public class SimpleAuthorizationListener implements AuthorizationListener {
	
	interface Params {
		String Token = "token";
	}
	
	interface Spec {
		String Peers 	= "peers";
		String Key 		= "key";
	}
	
	private JsonObject peers;
	
	@Override
	public boolean isAuthorized (HandshakeData data) {
		
		String token = data.getSingleUrlParam (Params.Token);
		
		if (Lang.isNullOrEmpty(token)) {
			return false;
		}

		token = token.trim ();
		
		int indexOfColon = token.indexOf (Lang.COLON);
		if (indexOfColon <= 0) {
			return false;
		}
		
		String peer 		= token.substring (0, indexOfColon);
		String key 			= token.substring (indexOfColon + 1);
		if (Lang.isNullOrEmpty (peer) || Lang.isNullOrEmpty (key)) {
			return false;
		}
		
		JsonObject oPeer = Json.getObject (peers, peer);
		if (Json.isNullOrEmpty (oPeer)) {
			return false;
		}
		
		String tenant = Json.getString (oPeer, Peer.Spec.Tenant);
		if (Lang.isNullOrEmpty (tenant)) {
			return false;
		}
		
		String tenantParam = data.getSingleUrlParam (SelectiveAuthorizationListener.TenantParameter);
		if (!tenant.equals (tenantParam)) {
			return false;
		}
		
		String type = Json.getString (oPeer, Peer.Spec.Type);
		if (Lang.isNullOrEmpty (type)) {
			type = Peer.Type.unknown.name ();
		}
		
		data.getUrlParams ().put (Peer.Key, PeerUtils.toList (peer, oPeer));
		
		return key.equals (Json.getString (oPeer, Spec.Key));
	}

	public JsonObject getPeers() {
		return peers;
	}

	public void setPeers(JsonObject peers) {
		this.peers = peers;
	}

}

package com.bluenimble.platform.servers.broker.listeners.auth;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.listeners.AbstractListener;
import com.bluenimble.platform.servers.broker.utils.PeerUtils;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;

public class SimpleAuthorizationListener extends AbstractListener implements AuthorizationListener {
	
	interface Params {
		String Token = "token";
	}
	
	interface Spec {
		String Peers 	= "peers";
		String Key 		= "key";
	}
	
	private JsonObject spec;
	
	public SimpleAuthorizationListener (JsonObject spec) {
		super (null);
		this.spec = spec;
	}

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
		
		JsonObject oPeer = (JsonObject)Json.find (spec, Spec.Peers, peer);
		if (Json.isNullOrEmpty (oPeer)) {
			return false;
		}
		
		String type = Json.getString (oPeer, Peer.Spec.Type);
		if (Lang.isNullOrEmpty (type)) {
			type = Peer.Type.consumer.name ();
		}
		
		data.getUrlParams ().put (Peer.Key, PeerUtils.toList (peer, oPeer));
		
		return key.equals (Json.getString (oPeer, Spec.Key));
	}

	@Override
	public void onData (SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {

	}
}

package com.bluenimble.platform.servers.socketio.listeners.auth;

import java.util.Arrays;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.socketio.listeners.AbstractListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;

public class SimpleAuthorizationListener extends AbstractListener implements AuthorizationListener {
	
	interface Spec {
		String Peers 	= "peers";
		String Key 		= "key";
		String Type		= "type";
	}
	
	private JsonObject spec;
	
	public SimpleAuthorizationListener (JsonObject spec) {
		super (null);
		this.spec = spec;
	}

	@Override
	public boolean isAuthorized (HandshakeData data) {
		
		String token = data.getSingleUrlParam (AbstractListener.Spec.Peer.Token);
		
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
		
		String type = Json.getString (oPeer, Spec.Type);
		if (Lang.isNullOrEmpty (type)) {
			type = PeerType.consumer.name ();
		}
		
		data.getUrlParams ().put (AbstractListener.Spec.Peer.Type, Arrays.asList (type));
		
		return key.equals (Json.getString (oPeer, Spec.Key));
	}

	@Override
	public void onData (SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {

	}
}

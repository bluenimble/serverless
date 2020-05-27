package com.bluenimble.platform.servers.broker.server.impls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.utils.PeerUtils;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DisconnectListener;

public class OnDisconnectListener implements DisconnectListener {
	
	private static final Logger logger = LoggerFactory.getLogger (OnDisconnectListener.class);
	
	public OnDisconnectListener (SocketIOServer server) {
	}

	@Override
	public void onDisconnect (final SocketIOClient client) {
		logger.info ("Client " + client.getSessionId () + " disconnected");
		Peer peer = PeerUtils.peer (client);
		
		if (peer == null) {
			logger.error ("Can not find peer for " + client.getSessionId () + " disconned");
			return;
		}
		if (Lang.isNullOrEmpty (peer.notifyOnDisconnect ())) {
			return;
		}
		peer.send (EventListener.Default.disconnected.name (), peer.notifyOnDisconnect (), new JsonObject ().set (Peer.Spec.Id, peer.id ()));
    }

}
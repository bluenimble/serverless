package com.bluenimble.platform.servers.broker.server.impls;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.Response;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.utils.PeerUtils;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;

public class OnConnectListener implements ConnectListener {
	
	private SocketIONamespace namespace;
	private SocketIOServer server;
	
	public OnConnectListener (SocketIOServer server, SocketIONamespace namespace) {
		this.server 	= server;
		this.namespace 	= namespace;
	}
	
	@Override
	public void onConnect (final SocketIOClient client) {
		Peer peer = PeerUtils.resolve (client.getHandshakeData ());
		if (peer == null) {
			client.sendEvent (EventListener.Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized access"));
			return;
		}
		
		peer.init (server, namespace, client);
		
		client.set (Peer.Key, peer);
		
		client.sendEvent (EventListener.Default.peer.name (), peer.info ());
    }

}
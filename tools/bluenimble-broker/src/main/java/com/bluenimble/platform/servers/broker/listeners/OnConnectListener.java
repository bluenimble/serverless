package com.bluenimble.platform.servers.broker.listeners;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.Response;
import com.bluenimble.platform.servers.broker.server.Broker.Events;
import com.bluenimble.platform.servers.broker.utils.PeerUtils;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;

public class OnConnectListener extends AbstractListener implements ConnectListener {
	
	public OnConnectListener (SocketIOServer server) {
		super (server);
	}
	
	@Override
	public void onConnect (final SocketIOClient client) {
		Peer peer = PeerUtils.resolve (client.getHandshakeData ());
		if (peer == null) {
			client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized access"));
			return;
		}
		client.set (Peer.Key, peer);
    }
	
	@Override
    public void onData (SocketIOClient client, JsonObject message, AckRequest ackRequest) {
    }

}

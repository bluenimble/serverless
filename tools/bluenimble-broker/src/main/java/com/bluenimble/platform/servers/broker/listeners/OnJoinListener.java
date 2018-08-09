package com.bluenimble.platform.servers.broker.listeners;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.Response;
import com.bluenimble.platform.servers.broker.server.Broker.Events;
import com.bluenimble.platform.servers.broker.utils.PeerUtils;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

public class OnJoinListener extends AbstractListener {

	public OnJoinListener (SocketIOServer server) {
		super (server);
	}
	
	@Override
    public void onData (SocketIOClient client, JsonObject message, AckRequest ackRequest) {
		
		Peer peer = PeerUtils.peer (client);
		
		if (!peer.isConsumer ()) {
			client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized action (SUB-001)"));
			return;
		}
		
		String channel = Json.getString (message, Message.Channel);
		if (Lang.isNullOrEmpty (channel)) {
			return;
		}
		
		if (!PeerUtils.canJoin (peer, client.getAllRooms (), channel)) {
			client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized action (SUB-002)"));
			return;
		}
        
		client.joinRoom (channel);
    }

}

package com.bluenimble.platform.servers.broker.listeners;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.Response;
import com.bluenimble.platform.servers.broker.server.Broker.Events;
import com.bluenimble.platform.servers.broker.utils.BrokerUtils;
import com.bluenimble.platform.servers.broker.utils.PeerUtils;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

public class OnPublishListener extends AbstractListener {

	public OnPublishListener (SocketIOServer server) {
		super (server);
	}
	
	@Override
    public void onData (SocketIOClient client, JsonObject message, AckRequest ackRequest) {
		if (!message.containsKey (Message.Data)) {
			client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Invalid Data (PUB-001)"));
			return;
		}
		
		Peer peer = PeerUtils.peer (client);
		
		if (!peer.isProducer ()) {
			client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized action (PUB-002)"));
			return;
		}
		
		Object oChannel = message.get (Message.Channel);
		if (oChannel == null) {
			client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Invalid data (PUB-003)"));
			return;
		}
		
		if (oChannel instanceof String) {
			String channel = (String)oChannel;
			if (Lang.isNullOrEmpty (channel)) {
				client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Invalid data (PUB-004)"));
				return;
			}
			if (!PeerUtils.canPublish (peer, channel)) {
				client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized action (PUB-005)"));
				return;
			}
			broadcast (channel, message.get (Message.Data));
		} else if (oChannel instanceof JsonArray) {
			JsonArray channels = (JsonArray)oChannel;
			if (channels.isEmpty ()) {
				client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Invalid data (PUB-004)"));
				return;
			}
			if (!PeerUtils.canPublish (peer, channels)) {
				client.sendEvent (Events.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized action (PUB-005)"));
				return;
			}
			for (Object oc : channels) {
				broadcast (String.valueOf (oc), message.get (Message.Data));
			}
		}
		
		if (ackRequest != null && ackRequest.isAckRequested ()) {
			ackRequest.sendAckData (new JsonObject ().set (Message.Status, Response.Success).set (Message.Timestamp, Lang.utc ()));
		}
		
		// kickout if not durable
		if (!peer.isDurable ()) {
			BrokerUtils.kickout (client, 1000);
		} 
		
    }
	
	private void broadcast (String channel, Object data) {
		BroadcastOperations ops = server.getRoomOperations (channel);
		if (ops == null) {
			return;
		}
		ops.sendEvent (Events.message.name (), data);
	}

}

package com.bluenimble.platform.servers.broker.listeners;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.server.Broker.Events;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

public class OnJoinListener extends AbstractListener {

	public OnJoinListener (SocketIOServer server) {
		super (server);
	}
	
	@Override
    public void onData (SocketIOClient client, JsonObject message, AckRequest ackRequest) {
		if (!isConsumer (client)) {
			client.sendEvent (Events.error.name (), new JsonObject ().set (Spec.Message.Status, Status.error).set (Spec.Message.Reason, "Unauthorized action"));
			return;
		}
		String channel = Json.getString (message, Spec.Message.Channel);
		if (Lang.isNullOrEmpty (channel)) {
			return;
		}
        client.joinRoom (channel);
    }

}

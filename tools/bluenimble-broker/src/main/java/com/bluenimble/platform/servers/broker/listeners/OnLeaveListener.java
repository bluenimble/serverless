package com.bluenimble.platform.servers.broker.listeners;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

public class OnLeaveListener extends AbstractListener {

	public OnLeaveListener (SocketIOServer server) {
		super (server);
	}
	
	@Override
    public void onData (SocketIOClient client, JsonObject message, AckRequest ackRequest) {
		String channel = Json.getString (message, Message.Channel);
		if (Lang.isNullOrEmpty (channel)) {
			return;
		}
        client.leaveRoom (channel);
    }

}

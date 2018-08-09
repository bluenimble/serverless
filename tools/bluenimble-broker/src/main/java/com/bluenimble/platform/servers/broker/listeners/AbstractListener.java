package com.bluenimble.platform.servers.broker.listeners;

import com.bluenimble.platform.json.JsonObject;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;

public abstract class AbstractListener implements DataListener<JsonObject> {

	protected SocketIOServer server;
	
	public AbstractListener (SocketIOServer server) {
		this.server = server;
	}

}

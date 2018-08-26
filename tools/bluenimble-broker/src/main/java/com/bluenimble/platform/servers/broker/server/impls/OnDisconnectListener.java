package com.bluenimble.platform.servers.broker.server.impls;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DisconnectListener;

public class OnDisconnectListener implements DisconnectListener {
	
	public OnDisconnectListener (SocketIOServer server) {
	}

	@Override
	public void onDisconnect (final SocketIOClient client) {
    }

}
package com.bluenimble.platform.servers.socketio.listeners;

import com.bluenimble.platform.json.JsonObject;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;

public class OnConnectListener extends AbstractListener implements ConnectListener {
	
	public OnConnectListener (SocketIOServer server) {
		super (server);
	}
	
	@Override
	public void onConnect (final SocketIOClient client) {
		HandshakeData hd = client.getHandshakeData ();
		String sType = hd.getSingleUrlParam (Spec.Peer.Type).trim ().toLowerCase ();
		client.set (Spec.Peer.Type, PeerType.valueOf (sType).name ());
    }
	
	@Override
    public void onData (SocketIOClient client, JsonObject message, AckRequest ackRequest) {
    }

}

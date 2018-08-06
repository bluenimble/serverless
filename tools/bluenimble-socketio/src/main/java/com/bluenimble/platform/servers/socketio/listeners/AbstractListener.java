package com.bluenimble.platform.servers.socketio.listeners;

import com.bluenimble.platform.json.JsonObject;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;

public abstract class AbstractListener implements DataListener<JsonObject> {

	protected enum PeerType {
		producer,
		consumer,
		both
	}
	
	protected enum Status {
		error,
		success
	}
	
	protected interface Spec {
		interface Peer {
			String Type 		= "type";
			String Token 		= "token";
			String Authorized 	= "authorized";
		}
		interface Message {
			String Channel 	= "channel";
			String Data 	= "data";
			String Status	= "status";
			String Reason	= "reason";
		}
	}
	
	protected SocketIOServer server;
	
	public AbstractListener (SocketIOServer server) {
		this.server = server;
	}
	
	protected boolean isConsumer (SocketIOClient client) {
		String peerType = client.get (Spec.Peer.Type);
		return PeerType.consumer.name ().equals (peerType) || PeerType.both.name ().equals (peerType);
	}
	
	protected boolean isProducer (SocketIOClient client) {
		String peerType = client.get (Spec.Peer.Type);
		return PeerType.producer.name ().equals (peerType) || PeerType.both.name ().equals (peerType);
	}
	
	protected void setTimeout (Runnable runnable, int delay){
	    new Thread(() -> {
	        try {
	            Thread.sleep(delay);
	            runnable.run();
	        }
	        catch (Exception e){
	            // Ignore
	        }
	    }).start();
	}

}

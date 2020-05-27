package com.bluenimble.platform.servers.broker.listeners;

import java.io.Serializable;

import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.PeerAck;

public interface EventListener<T> extends Serializable {

	enum Default {
		connect,
		join,
		leave,
		publish,
		disconnected,
		message,
		error,
		peer
	}
	
	void 		process (Peer peer, T message, PeerAck ack);
	
	String		name	();
	
	Class<?> 	dataType ();	
	
}

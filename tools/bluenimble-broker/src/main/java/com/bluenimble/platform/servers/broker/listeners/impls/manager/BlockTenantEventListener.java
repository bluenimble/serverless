package com.bluenimble.platform.servers.broker.listeners.impls.manager;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.PeerAck;
import com.bluenimble.platform.servers.broker.listeners.EventListener;

public class BlockTenantEventListener implements EventListener<JsonObject> {

	private static final long serialVersionUID = 4331863772457952104L;
	
	@Override
	public void process (Peer peer, JsonObject message, PeerAck ack) {
		
	}

	@Override
	public Class<?> dataType () {
		return JsonObject.class;
	}

	@Override
	public String name () {
		return "blockTenant";
	}

}

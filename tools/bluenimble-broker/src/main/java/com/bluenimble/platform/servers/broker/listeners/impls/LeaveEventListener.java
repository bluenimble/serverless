package com.bluenimble.platform.servers.broker.listeners.impls;

import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.PeerAck;
import com.bluenimble.platform.servers.broker.Response;
import com.bluenimble.platform.servers.broker.listeners.EventListener;

public class LeaveEventListener implements EventListener<JsonObject> {

	private static final long serialVersionUID = 4331863772457952104L;

	@Override
	public void process (Peer peer, JsonObject message, PeerAck ack) {
		
		Object oChannel = message.get (Message.Channel);
		if (oChannel == null) {
			peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Missing channel parameter"));
			return;
		}
		
		if (oChannel instanceof String) {
			peer.leave ((String)oChannel);
		} else if (oChannel instanceof JsonArray) {
			JsonArray channels = (JsonArray)oChannel;
			if (channels.isEmpty ()) {
				peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Missing channel parameter"));
				return;
			}
			for (Object oc : channels) {
				peer.leave (String.valueOf (oc));
			}
		}
		
	}

	@Override
	public Class<?> dataType () {
		return JsonObject.class;
	}

}

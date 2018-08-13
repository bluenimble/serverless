package com.bluenimble.platform.servers.broker.listeners.impls;

import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.PeerAck;
import com.bluenimble.platform.servers.broker.Response;
import com.bluenimble.platform.servers.broker.listeners.EventListener;

public class JoinEventListener implements EventListener<JsonObject> {

	private static final long serialVersionUID = 4331863772457952104L;
	
	@Override
	public void process (Peer peer, JsonObject message, PeerAck ack) {
		
		Object oChannel = message.get (Message.Channel);
		if (oChannel == null) {
			peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Missing channel parameter"));
			return;
		}
		
		if (oChannel instanceof String) {
			if (!peer.canJoin ((String)oChannel)) {
				peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized action"));
				return;
			}
			peer.join ((String)oChannel);
		} else if (oChannel instanceof JsonArray) {
			JsonArray channels = (JsonArray)oChannel;
			if (channels.isEmpty ()) {
				peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Missing channel parameter"));
				return;
			}
			List<String> cantJoin = null;
			for (Object oc : channels) {
				if (!peer.canJoin ((String)oChannel)) {
					if (cantJoin == null) {
						cantJoin = new ArrayList<String> ();
					}
					cantJoin.add (String.valueOf (oc));
				} else {
					peer.join (String.valueOf (oc));
				}
			}
			if (cantJoin != null) {
				peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Cant join " + Lang.join (cantJoin)));
			}
		}
		
	}

	@Override
	public Class<?> dataType () {
		return JsonObject.class;
	}

}

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

public class PublishEventListener implements EventListener<JsonObject> {

	private static final long serialVersionUID = 4331863772457952104L;
	
	@Override
	public void process (Peer peer, JsonObject message, PeerAck ack) {
		
		if (!message.containsKey (Message.Data)) {
			peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "No Data to broadcast"));
			return;
		}
		
		Object oChannel = message.get (Message.Channel);
		if (oChannel == null) {
			peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Missing channel parameter"));
			return;
		}
		
		if (oChannel instanceof String) {
			String channel = (String)oChannel;
			if (Lang.isNullOrEmpty (channel)) {
				peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Missing channel parameter"));
				return;
			}
			if (!peer.canPublish (channel)) {
				peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Unauthorized action"));
				return;
			}
			peer.broadcast (channel, message.get (Message.Data));
		} else if (oChannel instanceof JsonArray) {
			JsonArray channels = (JsonArray)oChannel;
			if (channels.isEmpty ()) {
				peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Missing channel parameter"));
				return;
			}
			
			List<String> cantJoin = null;
			for (Object oc : channels) {
				if (!peer.canJoin ((String)oc)) {
					if (cantJoin == null) {
						cantJoin = new ArrayList<String> ();
					}
					cantJoin.add (String.valueOf (oc));
				} else {
					peer.broadcast (String.valueOf (oc), message.get (Message.Data));
				}
			}
			if (cantJoin != null) {
				peer.trigger (Default.error.name (), new JsonObject ().set (Message.Status, Response.Error).set (Message.Reason, "Cant pubish to " + Lang.join (cantJoin)));
			}
		}
		
		if (ack.requested ()) {
			ack.notify (new JsonObject ().set (Message.Status, Response.Success).set (Message.Timestamp, Lang.utc ()));
		}
		
		// kickout if not durable
		if (!peer.isDurable ()) {
			peer.terminate (1000);
		} 
		
	}

	@Override
	public Class<?> dataType () {
		return JsonObject.class;
	}

}

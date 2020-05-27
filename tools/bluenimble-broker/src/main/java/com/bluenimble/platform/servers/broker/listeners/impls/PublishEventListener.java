package com.bluenimble.platform.servers.broker.listeners.impls;

import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.PeerAck;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.utils.MessagingUtils;

public class PublishEventListener implements EventListener<JsonObject> {
	
	private static final long serialVersionUID = 4331863772457952104L;
	
	@Override
	public void process (Peer peer, JsonObject message, PeerAck ack) {
		
		Object transaction = message.get (Message.Transaction);
		
		if (!message.containsKey (Message.Data)) {
			peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "No Data to broadcast"));
			return;
		}
		
		Object oChannel = message.get (Message.Channel);
		if (oChannel == null) {
			peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Missing channel parameter"));
			return;
		}
		
		boolean refreshPeer = Json.getBoolean (message, Message.RefreshPeer, false);
		if (refreshPeer) {
			peer.refresh ();
		}
		
		if (oChannel instanceof String) {
			String channel = (String)oChannel;
			if (Lang.isNullOrEmpty (channel)) {
				peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Missing channel parameter"));
				return;
			}
			if (!peer.canPublish (channel)) {
				peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Unauthorized action"));
				return;
			}
			peer.broadcast (channel, message.get (Message.Data));
		} else if (oChannel instanceof JsonArray) {
			JsonArray channels = (JsonArray)oChannel;
			if (channels.isEmpty ()) {
				peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Missing channel parameter"));
				return;
			}
			
			List<String> cantPublish = null;
			for (Object oc : channels) {
				if (!peer.canPublish ((String)oc)) {
					if (cantPublish == null) {
						cantPublish = new ArrayList<String> ();
					}
					cantPublish.add (String.valueOf (oc));
				} else {
					peer.broadcast (String.valueOf (oc), message.get (Message.Data));
				}
			}
			if (cantPublish != null) {
				peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Cant publish to " + Lang.join (cantPublish)));
			}
		}
		
		if (ack.requested ()) {
			ack.notify (MessagingUtils.createSuccess (name (), transaction, Lang.utc ()));
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

	@Override
	public String name () {
		return "publish";
	}

}

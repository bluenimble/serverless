package com.bluenimble.platform.servers.broker.listeners.impls;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.PeerAck;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.utils.MessagingUtils;

public class LeaveEventListener implements EventListener<JsonObject> {

	private static final long serialVersionUID = 4331863772457952104L;

	@Override
	public void process (Peer peer, JsonObject message, PeerAck ack) {
		
		Object transaction = message.get (Message.Transaction);
		
		Object oChannel = message.get (Message.Channel);
		if (oChannel == null) {
			peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Missing channel parameter"));
			return;
		}
		
		if (oChannel instanceof String) {
			peer.leave ((String)oChannel);
		} else if (oChannel instanceof JsonArray) {
			JsonArray channels = (JsonArray)oChannel;
			if (channels.isEmpty ()) {
				peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Missing channel parameter"));
				return;
			}
			for (Object oc : channels) {
				peer.leave (String.valueOf (oc));
			}
		}
		
		if (ack.requested ()) {
			ack.notify (MessagingUtils.createSuccess (name (), transaction, Lang.utc ()));
		}
		
	}

	@Override
	public Class<?> dataType () {
		return JsonObject.class;
	}

	@Override
	public String name () {
		return "leave";
	}

}

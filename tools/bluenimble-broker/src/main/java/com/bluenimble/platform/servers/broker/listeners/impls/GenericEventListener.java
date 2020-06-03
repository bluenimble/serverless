package com.bluenimble.platform.servers.broker.listeners.impls;

import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.PeerAck;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.utils.MessagingUtils;

public class GenericEventListener implements EventListener<GenericObject> {
	
	private static final long serialVersionUID = 4331863772457952104L;
	
	@Override
	public void process (Peer peer, GenericObject message, PeerAck ack) {
		
		Object transaction = message.getTransaction ();
		
		if (message.getEvent () == null) {
			peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "No Event to process"));
			return;
		}
		
		if (message.getData () == null) {
			peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "No Data to process (" + message.getEvent () + ")"));
			return;
		}
		
		if (message.getChannel () == null || message.getChannel ().length == 0) {
			peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Missing channel parameter"));
			return;
		}
		
		if (message.isRefreshPeer ()) {
			peer.refresh ();
		}
		
		List<String> cantPublish = null;
		for (String oc : message.getChannel ()) {
			if (!peer.canPublish (oc)) {
				if (cantPublish == null) {
					cantPublish = new ArrayList<String> ();
				}
				cantPublish.add (oc);
			} else {
				peer.send (message.getEvent (), oc, message.getData ());
			}
		}
		if (cantPublish != null) {
			peer.trigger (Default.error.name (), MessagingUtils.createError (name (), transaction, "Cant send (" + message.getEvent () + ") to " + Lang.join (cantPublish)));
		}
		
		if (ack.requested ()) {
			ack.notify (MessagingUtils.createSuccess (name (), transaction, Lang.utc ()));
		}
		
	}

	@Override
	public Class<?> dataType () {
		return GenericObject.class;
	}

	@Override
	public String name () {
		return "generic";
	}

}

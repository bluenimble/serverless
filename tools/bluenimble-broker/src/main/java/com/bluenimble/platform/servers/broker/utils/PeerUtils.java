package com.bluenimble.platform.servers.broker.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.impls.PeerImpl;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;

public class PeerUtils {
	
	public static Peer create () {
		return new PeerImpl ();
	}
	
	public static Peer create (List<String> data) {
		Peer peer = new PeerImpl ();
		if (data == null || data.isEmpty ()) {
			return peer;
		}
		
		setId (peer, data.get (0));

		setType (peer, data.get (1));
		
		if (data.size () > 2) {
			setDurable (peer, data.get (2));
		}
		
		if (data.size () > 3) {
			setMonoChannel (peer, data.get (3));
		}
		
		if (data.size () > 4) {
			String [] channels = Lang.split (data.get (4), Lang.SPACE, true);
			if (channels != null && channels.length > 0) {
				for (String channel : channels) {
					if (Lang.isNullOrEmpty (channel)) {
						continue;
					}
					peer.addChannel (channel);
				}
			}
		}
		
		return peer;
	}
	
	public static Peer resolve (HandshakeData hd) {
		Map<String, List<String>> params = hd.getUrlParams ();
		if (params == null || params.isEmpty () || !params.containsKey (Peer.Key)) {
			return null;
		}
		return create (params.get (Peer.Key));
	}
	
	public static final Peer peer (SocketIOClient client) {
		return (Peer)client.get (Peer.Key);
	}
	
	public static void setId (Peer peer, String id) {
		if (Lang.isNullOrEmpty (id)) {
			return;
		}
		peer.id (id.trim ());
	}
	
	private static void setType (Peer peer, String sType) {
		if (Lang.isNullOrEmpty (sType)) {
			return;
		}
		peer.type (sType.trim ());
	}
	
	private static void setDurable (Peer peer, String sDurable) {
		boolean durable = true;
		if (!Lang.isNullOrEmpty (sDurable)) {
			durable = Lang.TrueValues.contains (sDurable);
		}
		peer.setDurable (durable);
	}
	private static void setMonoChannel (Peer peer, String sMonoChannel) {
		peer.setMonoChannel (Lang.TrueValues.contains (sMonoChannel));
	}
	
	public static List<String> toList (String id, JsonObject data) {
		if (Lang.isNullOrEmpty (id)) {
			return null;
		} 
		List<String> list = new ArrayList<String> ();
		list.add (id);
		
		if (Json.isNullOrEmpty (data)) {
			return list;
		}
		
		list.add (Json.getString (data, Peer.Spec.Type));
		list.add (Json.getString (data, Peer.Spec.Durable));
		list.add (Json.getString (data, Peer.Spec.MonoChannel));
		
		JsonArray channels = Json.getArray (data, Peer.Spec.Channels);
		if (!Json.isNullOrEmpty (channels)) {
			list.add (Json.getArray (data, Peer.Spec.Channels).join (Lang.SPACE));
		}
		
		return list;
		
	}
	
}

package com.bluenimble.platform.servers.broker.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.Peer.Type;
import com.bluenimble.platform.servers.broker.impls.PeerImpl;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;

public class PeerUtils {
	
	private static final Set<String> EmptySet = new HashSet<String> ();
	
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
	
	public static void setType (Peer peer, String sType) {
		if (Lang.isNullOrEmpty (sType)) {
			return;
		}
		
		Type type = Type.consumer;

		sType = sType.trim ();
		try {
			type = Type.valueOf (sType);
		} catch (Exception ex) {
			// ignore
		}
		
		peer.type (type);
	}
	
	public static void setDurable (Peer peer, String sDurable) {
		boolean durable = true;
		if (!Lang.isNullOrEmpty (sDurable)) {
			durable = Lang.TrueValues.contains (sDurable);
		}
		peer.setDurable (durable);
	}
	public static void setMonoChannel (Peer peer, String sMonoChannel) {
		peer.setMonoChannel (Lang.TrueValues.contains (sMonoChannel));
	}
	
	public static boolean canJoin (SocketIOClient client, String channel) {
		return canJoin (peer (client), client.getAllRooms (), channel);
	}
	
	public static boolean canJoin (Peer peer, Set<String> joined, String channel) {
		if (joined == null) {
			joined = EmptySet;
		}
		
		if (joined.contains (channel)) {
			return false;
		}
		
		// mono
		if (!joined.isEmpty () && peer.isMonoChannel ()) {
			return false;
		}
		
		// has right to channel
		if (!peer.hasChannel (channel)) {
			return false;
		}
		
		return true;
	}
	
	public static boolean canPublish (SocketIOClient client, String channel) {
		return canPublish (peer (client), channel);
	}
	
	public static boolean canPublish (Peer peer, String channel) {
		// has right to channel
		if (!peer.hasChannel (channel)) {
			return false;
		}
		return true;
	}
	
	public static boolean canPublish (Peer peer, JsonArray channels) {
		if (peer.hasChannel (null)) {
			return true;
		}
		for (Object oc : channels) {
			boolean can = canPublish (peer, String.valueOf (oc));
			if (!can) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean canPublish (SocketIOClient client, JsonArray channels) {
		return canPublish (peer (client), channels);
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

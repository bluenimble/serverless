package com.bluenimble.platform.servers.broker.impls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.PeerChannel;
import com.bluenimble.platform.servers.broker.PeerChannel.Access;
import com.bluenimble.platform.servers.broker.Tenant;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

public class PeerImpl implements Peer {

	private static final long serialVersionUID = 4588408497056063439L;
	
	private static final Logger logger = LoggerFactory.getLogger (PeerImpl.class);
	
	private static final String AnyChannel = Lang.STAR;
	
	private static final Set<String> EmptySet = new HashSet<String> ();
	
	interface Spec {
		String UUID 		= "uuid";
		String Type 		= "type";
		String Durable 		= "durable";
		String Channels 	= "channels";
		String MonoChannel 	= "monoChannel";
		interface Channel {
			String Name 		= "name";
			String Access 		= "access";
		}
	}
	
	protected String 			id;
	protected String 			type;
	protected String 			tenantId;
	protected Tenant 			tenant;
	
	protected boolean 			durable = true;
	protected Map<String, PeerChannel> 	
								channels;
	protected boolean 			monoChannel;
	
	private transient SocketIOServer 		server;
	private transient SocketIOClient 		client;
	
	@Override
	public void init (Broker broker, SocketIOServer server, SocketIOClient client) {
		this.server = broker != null ? broker.server () : server;
		this.tenant = broker != null ? broker.getTenantProvider ().get (tenantId) : null;
		this.client = client;
	}
	
	public String id () {
		return id;
	}
	
	@Override
	public void type (String type) {
		this.type = type;
	}
	@Override
	public String type () {
		return type;
	}

	@Override
	public Tenant tenant () {
		return tenant;
	}
	@Override
	public void tenant (String tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public boolean isDurable () {
		return durable;
	}

	@Override
	public Map<String, PeerChannel> channels () {
		return channels;
	}

	@Override
	public boolean isMonoChannel () {
		return monoChannel;
	}
	
	@Override
	public boolean is (Set<String> peerTypes) {
		if (Type.unknown.name ().equals (type)) {
			return false;
		}
		if (peerTypes == null || peerTypes.isEmpty ()) {
			return true;
		}
		return Type.joker.name ().equals (type) || peerTypes.contains (type);
	}

	@Override
	public boolean isNode () {
		return Type.node.name ().equals (type);
	}

	@Override
	public void id (String id) {
		this.id = id;
	}

	@Override
	public void setDurable (boolean durable) {
		this.durable = durable;
	}

	@Override
	public void setMonoChannel (boolean monoChannel) {
		this.monoChannel = monoChannel;
	}

	@Override
	public void addChannel (PeerChannel channel) {
		if (channels == null) {
			channels = new HashMap<String, PeerChannel> ();
		}
		channels.put (channel.name (), channel);
	}
	
	@Override
	public boolean hasAccess (String channel, PeerChannel.Access access) {
		// no defined channels, peer has right to all channels to execute actions
		if (channels == null || channels.isEmpty ()) {
			return false;
		}
		if (Lang.isNullOrEmpty (channel)) {
			return false;
		}
		
		if (channels.containsKey (AnyChannel)) {
			return true;
		}
		
		PeerChannel peerChannel = channels.get (channel);
		if (peerChannel != null) {
			if (peerChannel.access ().equals (Access.All)) {
				return true;
			}
			return peerChannel.access ().equals (access);
		}
		
		boolean hasAccess = false;
		
		// wildcard
		for (PeerChannel wildcard : channels.values ()) {
			hasAccess = Lang.wmatches (wildcard.name (), channel);
			if (hasAccess) {
				break;
			}
		}
		
		return hasAccess;
		
	}

	@Override
	public void trigger (String event, Object... data) {
		if (client == null) {
			return;
		}
		client.sendEvent (event, data);
	}

	@Override
	public void join (String channel) {
		/*
		if (joined ().contains (channel)) {
			return;
		}
		*/
		client.joinRoom (tenantId + Lang.SLASH + channel);
	}

	@Override
	public void leave (String channel) {
		client.leaveRoom (tenantId + Lang.SLASH + channel);
	}

	@Override
	public Set<String> joined () {
		Set<String> joined = client.getAllRooms ();
		if (joined == null) {
			return EmptySet;
		}
		return joined;
	}
	
	@Override
	public boolean canJoin (String channel) {
		
		/*
		Set<String> joined = joined ();
		
		if (joined.contains (channel)) {
			return false;
		}
		*/
		
		// is mono
		if (!joined ().isEmpty () && isMonoChannel ()) {
			return false;
		}
		
		return hasAccess (channel, Access.Read);
	}
	
	@Override
	public boolean canPublish (String channel) {
		return hasAccess (channel, Access.Write); 
	}
	
	@Override
	public void terminate (int delay) {
		if (delay == 0) {
			client.disconnect ();
			return;
		}
		Lang.setTimeout (new Runnable () {
			@Override
			public void run () {
				client.disconnect ();
			}
		}, delay);
	}

	@Override
	public void broadcast (String channel, Object data) {
		if (tenant != null && tenant.namespacedBroadcast ()) {
			channel = tenant.id () + Lang.SLASH + channel;
		}
		logger.info ("broadcast to " + channel);
		BroadcastOperations ops = server.getRoomOperations (channel);
		if (ops == null) {
			return;
		}
		
		ops.sendEvent (EventListener.Default.message.name (), data);
	}

	@Override
	public JsonObject info () {
		JsonObject info = new JsonObject ();
		
		if (client != null) {
			info.set (Spec.UUID, client.getSessionId ().toString ());
		}
		info.set (Spec.Type, type ());
		
		JsonArray aChannels = new JsonArray ();
		if (channels != null) {
			for (PeerChannel pc : channels.values ()) {
				JsonObject opc = new JsonObject ();
				opc.set (Spec.Channel.Name, pc.name ());
				opc.set (Spec.Channel.Access, pc.access ().toString ());
				aChannels.add (opc);
			}
		}
		info.set (Spec.Channels, aChannels);
		
		info.set (Spec.Durable, isDurable ());
		info.set (Spec.MonoChannel, isMonoChannel ());
		
		return info;
	}
	
}

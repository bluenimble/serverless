package com.bluenimble.platform.servers.broker;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

public interface Peer extends Serializable {
	
	String Key = "Broker.Peer.Key";

	interface Spec {
		String Id 					= "id";
		String Token 				= "token";
		String Tenant 				= "tenant";
		String Type 				= "type";
		String Durable 				= "durable";
		String MonoChannel 			= "monoChannel";
		String NotifyOnDisconnect 	= "notifyOnDisconnect";
		String Channels 			= "channels";
		interface Channel {
			String Name 			= "name";
			String Access 			= "access";
		}
	}
	
	// default types
	enum Type {
		unknown,
		joker,
		node
	}
	
	String		id				();
	void		id				(String id);
	
	String		type			();
	void		type			(String type);
	
	String		token			();
	void		token			(String token);

	Tenant		tenant			();
	void		tenant			(String tenant);
	
	boolean 	isDurable 		();
	void		setDurable		(boolean durable);
	
	boolean 	isMonoChannel 	();
	void		setMonoChannel	(boolean monoChannel);
	
	String		notifyOnDisconnect ();
	void		notifyOnDisconnect (String channel);
	
	Map<String, PeerChannel> 
				channels 		();
	void		addChannel		(PeerChannel channel);
	boolean		hasAccess		(String channel, PeerChannel.Access access);
	
	boolean		is 				(Set<String> peerTypes);
	boolean		isNode 			();
	
	Set<String> joined 			();
	
	void		init 			(Broker broker, SocketIOServer server, SocketIOClient client);
	
	void		trigger 		(String event, Object... message);
	
	void		join 			(String channel);
	void		leave 			(String channel);
	
	void 		send 			(String event, String channel, Object data);
	
	void 		broadcast 		(String channel, Object data);
	
	boolean 	canJoin			(String channel);
	
	boolean 	canPublish		(String channel);
	
	void		terminate 		(int delay);
	
	void		refresh 		();
	
	JsonObject	info			();

}

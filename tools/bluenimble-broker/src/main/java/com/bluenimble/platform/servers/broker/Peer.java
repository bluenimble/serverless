package com.bluenimble.platform.servers.broker;

import java.io.Serializable;
import java.util.Set;

public interface Peer extends Serializable {
	
	String Key = "Broker.Peer.Key";

	interface Spec {
		String Id 			= "id";
		String Type 		= "type";
		String Durable 		= "durable";
		String MonoChannel 	= "monoChannel";
		String Channels 	= "channels";
	}
	
	enum Type {
		producer,
		consumer,
		both,
		node
	}
	
	String		id				();
	void		id				(String id);
	
	Type		type			();
	void		type			(Type type);
	
	boolean 	isDurable 		();
	void		setDurable		(boolean durable);
	
	boolean 	isMonoChannel 	();
	void		setMonoChannel	(boolean monoChannel);
	
	Set<String> channels 		();
	void		addChannel		(String channel);
	boolean		hasChannel		(String channel);
	
	boolean 	isConsumer 		();
	boolean 	isProducer 		();
	boolean 	isNode 			();

}

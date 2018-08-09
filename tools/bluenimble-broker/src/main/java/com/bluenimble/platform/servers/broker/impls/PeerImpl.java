package com.bluenimble.platform.servers.broker.impls;

import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.servers.broker.Peer;

public class PeerImpl implements Peer {

	private static final long serialVersionUID = 4588408497056063439L;
	
	protected String 		id;
	protected Type 			type;
	
	protected boolean 		durable = true;
	protected Set<String> 	channels;
	protected boolean 		monoChannel = false;
	
	public String id () {
		return id;
	}

	public Type type () {
		return type;
	}
	
	@Override
	public boolean isDurable () {
		return durable;
	}

	@Override
	public Set<String> channels () {
		return channels;
	}

	@Override
	public boolean isMonoChannel () {
		return monoChannel;
	}
	
	@Override
	public boolean isConsumer () {
		return Type.consumer.equals (type) || Type.both.equals (type) || Type.node.equals (type);
	}

	@Override
	public boolean isProducer () {
		return Type.producer.equals (type) || Type.both.equals (type) || Type.node.equals (type);
	}

	@Override
	public boolean isNode () {
		return Type.node.equals (type);
	}

	@Override
	public void id (String id) {
		this.id = id;
	}

	@Override
	public void type (Type type) {
		this.type = type;
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
	public void addChannel (String channel) {
		if (channels == null) {
			channels = new HashSet<String> ();
		}
		channels.add (channel);
	}
	
	@Override
	public boolean hasChannel (String channel) {
		// no defined channels, peer has right to all channels to join/publish
		if (channels == null) {
			return true;
		}
		if (Lang.isNullOrEmpty (channel)) {
			return false;
		}
		return channels.contains (channel);
	}
	
}

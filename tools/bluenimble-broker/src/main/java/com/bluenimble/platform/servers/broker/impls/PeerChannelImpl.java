package com.bluenimble.platform.servers.broker.impls;

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.servers.broker.PeerChannel;

public class PeerChannelImpl implements PeerChannel {
	
	private static final long serialVersionUID = -2100443742356661540L;

	interface AccessString {
		String Read 	= "r";
		String Write 	= "w";
		String All 		= "x";
	}
	
	private static final Map<String, Access> AccessMap = new HashMap<String, Access> ();
	static {
		AccessMap.put (AccessString.Read, Access.Read);
		AccessMap.put (AccessString.Write, Access.Write);
		AccessMap.put (AccessString.All, Access.All);
	}
	
	private String name;
	private Access access;
	
	public PeerChannelImpl (String spec) {
		this.name = spec;
		this.access = Access.All;
		int indexOfColon = spec.indexOf (Lang.COLON);
		if (indexOfColon > 0) {
			try {
				this.access = Access.valueOf (spec.substring (indexOfColon + 1));
			} catch (Exception ex) {
				// IGNORE
			}
			this.name = spec.substring (0, indexOfColon);
		}
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public Access access () {
		return access;
	}

}

package com.bluenimble.platform.servers.broker;

import java.io.Serializable;

public interface PeerChannel extends Serializable {
	
	enum Access {
		Read,
		Write,
		All
	}

	String 			name 		();
	Access 			access 		();
	
}

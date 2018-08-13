package com.bluenimble.platform.servers.broker;

import java.io.Serializable;

public interface PeerAck extends Serializable {

	boolean requested 	();
	void 	notify 		(Object... data);
	
}

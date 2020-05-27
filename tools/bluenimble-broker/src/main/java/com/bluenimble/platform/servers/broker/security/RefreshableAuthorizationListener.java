package com.bluenimble.platform.servers.broker.security;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;

public interface RefreshableAuthorizationListener {

	JsonObject refreshPeer (Peer peer);
	
}

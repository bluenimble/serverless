package com.bluenimble.platform.servers.broker;

import java.io.Serializable;
import java.util.List;

import com.bluenimble.platform.json.JsonObject;

public interface Tenant extends Serializable {

	String 			id 			();
	String 			name 		();
	
	boolean			namespacedBroadcast ();
	boolean			available 	();
	boolean			supports 	(String event);

	List<Object>	auths 		();
	JsonObject		toJson		();
	
}

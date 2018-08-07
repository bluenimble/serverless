package com.bluenimble.platform.servers.broker.server.tests;

import com.bluenimble.platform.servers.broker.server.Broker;
import com.bluenimble.platform.servers.broker.server.impls.BrokerImpl;
import com.bluenimble.platform.json.JsonObject;

public class ExampleBroker {

	public static void main (String [] args) throws Exception {
		Broker server = new BrokerImpl ((JsonObject)new JsonObject ().set (Broker.Spec.Host, "localhost"));
		server.start ();
	}
	
}

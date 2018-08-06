package com.bluenimble.platform.servers.socketio.server.tests;

import com.bluenimble.platform.servers.socketio.server.Server;
import com.bluenimble.platform.servers.socketio.server.impls.ServerImpl;
import com.bluenimble.platform.json.JsonObject;

public class ExampleServer {

	public static void main (String [] args) throws Exception {
		Server server = new ServerImpl ((JsonObject)new JsonObject ().set (Server.Spec.Host, "localhost"));
		server.start ();
	}
	
}

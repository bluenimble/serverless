package com.bluenimble.platform.servers.socketio.server.boot;

import java.io.File;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.servers.socketio.server.Server;
import com.bluenimble.platform.servers.socketio.server.impls.ServerImpl;

public class BootServer {

	public static void main (String [] args) throws Exception {
		Server server = new ServerImpl (Json.load (new File ("server.json")));
		server.start ();
	}
	
}

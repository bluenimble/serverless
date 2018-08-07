package com.bluenimble.platform.servers.broker;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.listeners.OnJoinListener;
import com.bluenimble.platform.servers.broker.listeners.OnPublishListener;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

public class Server {
	
	interface Events {
		String Auth 		= "auth";
		
		String Join 		= "join";
		String Leave 		= "leave";
		
		String Broadcast 	= "broadcast";
	}
	
	public static void main (String [] args) throws InterruptedException {

        Configuration config = new Configuration ();
        config.setHostname ("localhost");
        config.setPort (10443);

        final SocketIOServer server = new SocketIOServer (config);
        
        server.addEventListener ("join", JsonObject.class, new OnJoinListener (server));
        server.addEventListener ("message", JsonObject.class, new OnPublishListener (server));

        server.start ();

    }
	
}

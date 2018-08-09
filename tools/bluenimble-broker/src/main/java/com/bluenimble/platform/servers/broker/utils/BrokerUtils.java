package com.bluenimble.platform.servers.broker.utils;

import com.corundumstudio.socketio.SocketIOClient;

public class BrokerUtils {
	
	public static void kickout (SocketIOClient client, int delay) {
		if (delay == 0) {
			client.disconnect ();
			return;
		}
		setTimeout (new Runnable () {
			@Override
			public void run () {
				client.disconnect ();
			}
		}, delay);
	}
	
	public static void setTimeout (Runnable runnable, int delay){
	    new Thread(() -> {
	        try {
	            Thread.sleep (delay);
	            runnable.run();
	        }
	        catch (Exception e){
	            // Ignore
	        }
	    }).start();
	}
	
}

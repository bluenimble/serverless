package com.bluenimble.platform.remote.impls.http.tests;

import java.util.Map;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.Remote.Callback;
import com.bluenimble.platform.remote.impls.http.HttpRemote;

public class HttpRemoteTest {
	
	
	public static void main (String [] args) throws Exception {
		
		new HttpRemote ()
			.get (
				(JsonObject)new JsonObject ()
					.set (Remote.Spec.Endpoint, "http://localhost:9090")
					.set (Remote.Spec.Path, "/sys/mgm/instance/keys"),
					new Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
					System.err.println ("onHeaders\t : " + headers);
				}
				@Override
				public void onData (int code, byte [] data) {
				}
				@Override
				public void onError (int code, Object message) {
					System.err.println ("Error\n\t" + code + " : " + message);
				}
				@Override
				public void onDone (int code, Object data) {
					System.err.println ("onDone\n\t" + code + " :\n " + data);
				}
			});
	}
}


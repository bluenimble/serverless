package com.bluenimble.platform.icli.mgm.remote.binary.tests;

import com.bluenimble.platform.icli.mgm.remote.binary.BinaryClient;

public class ClientTest {

	public static void main (String[] args) throws Exception {
		BinaryClient client = new BinaryClient ("localhost", 7070);
		System.out.println ("Client Created");
		client.connect ();
		System.out.println ("Connected");
		/*
		client.send (
			new SimpleApiRequest ("binary", ApiVerb.POST, "bbps", "binaryserver", "/hello/teta", "AlphaOrigin", "Alpha"),
			new ResponseCallback () {
				@Override
				public void onStatus (ApiResponse.Status status) {
					System.out.println ("Status: " + status);
				}
				@Override
				public void onHeaders (Map<String, Object> headers) {
					System.out.println ("Headers: " + headers);
				}
				@Override
				public void onChunk (byte [] bytes) {
					System.out.println ("Chunk: \n" + bytes);
				}
				@Override
				public void onFinish () {
					System.out.println ("Done!");
				}
			}
		);
		*/
		
		for (int i = 0; i < 100; i++) {
			new Worker (client, String.valueOf (i)).start ();
		}
		
		//client.disconnect ();
	}	
}

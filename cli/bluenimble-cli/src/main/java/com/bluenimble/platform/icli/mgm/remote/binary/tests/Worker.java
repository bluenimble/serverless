package com.bluenimble.platform.icli.mgm.remote.binary.tests;

import java.util.Map;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.impls.SimpleApiRequest;
import com.bluenimble.platform.icli.mgm.remote.binary.BinaryClient;
import com.bluenimble.platform.icli.mgm.remote.binary.ResponseCallback;

public class Worker extends Thread {
	
	private String			id;
	private BinaryClient 	client;
	
	public Worker (BinaryClient client, String id) {
		this.client = client;
		this.id = id;
	}
	
	public void run () {
		client.send (
			new SimpleApiRequest ("binary", ApiVerb.POST, "bbps", "binaryserver", "/hello/teta-" + id, "AlphaOrigin", "Alpha"),
			new ResponseCallback () {
				@Override
				public void onStatus (ApiResponse.Status status) {
					System.out.println ("Status-" + id + ": " + status);
				}
				@Override
				public void onHeaders (Map<String, Object> headers) {
					System.out.println ("Headers-" + id + ": " + headers);
				}
				@Override
				public void onChunk (byte [] bytes) {
					System.out.println ("Chunk-" + id + ": \n" + bytes);
				}
				@Override
				public void onFinish () {
					System.out.println ("Done! " + id);
				}
			}
		);
	}
} 
package vom.bluenimble.platform.tools.binary.tests;

import java.util.Map;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.impls.SimpleApiRequest;

import vom.bluenimble.platform.tools.binary.BinaryClientFactory;
import vom.bluenimble.platform.tools.binary.Callback;

public class Worker extends Thread {
	
	private String					id;
	private BinaryClientFactory 	factory;
	
	public Worker (BinaryClientFactory factory, String id) {
		this.factory = factory;
		this.id = id;
	}
	
	public void run () {
		factory.create (new Callback () {
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
		}).send (
			new SimpleApiRequest ("binary", ApiVerb.POST, "bbps", "binaryserver", "/hello/teta-" + id, "AlphaOrigin", "Alpha")
		);
	}
} 
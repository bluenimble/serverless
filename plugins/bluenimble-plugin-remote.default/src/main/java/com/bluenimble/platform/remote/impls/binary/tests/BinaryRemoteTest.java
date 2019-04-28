package com.bluenimble.platform.remote.impls.binary.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import com.bluenimble.platform.ValueHolder;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.pooling.PoolConfig;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.Remote.Callback;
import com.bluenimble.platform.remote.impls.binary.BinaryRemote;
import com.bluenimble.platform.tools.binary.BinaryClientFactory;
import com.bluenimble.platform.tools.binary.impls.netty.NettyBinaryClientFactory;

public class BinaryRemoteTest {
	
	
	public static void main (String [] args) throws Exception {
		
		BinaryClientFactory factory = new NettyBinaryClientFactory (
			"localhost", 7070, 
			new PoolConfig ()
				.setPartitionSize (5)
				.setMinSize (5)
				.setMaxSize (10)
				.setMaxIdleMilliseconds(60 * 1000 * 5)
		);
	
		ValueHolder<ByteArrayOutputStream> stream = new ValueHolder<ByteArrayOutputStream> (new ByteArrayOutputStream ());
		
		new BinaryRemote (factory.create (), true)
			.get (
				(JsonObject)new JsonObject ()
					.set (Remote.Spec.Path, "/sys/mgm/instance/keys"),
					new Callback () {
				@Override
				public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
					System.out.println ("onHeaders\t : " + headers);
				}
				@Override
				public void onData (int code, byte [] data) throws IOException {
					if (data == null || data.length == 0) {
						return;
					}
					stream.get ().write (data);
				}
				@Override
				public void onError (int code, Object message) {
					System.out.println ("Error\n\t" + code + " : " + message);
				}
				@Override
				public void onDone (int code, Object data) {
					System.out.println ("onDone\n\t" + code + " :\n " + new String (stream.get ().toByteArray ()));
				}
			});
	}
}


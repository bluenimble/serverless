package com.bluenimble.platform.tools.binary.impls.netty.pool;

import com.bluenimble.platform.tools.binary.BinaryClient;
import com.bluenimble.platform.tools.binary.impls.netty.NettyBinaryClient;

public class BinaryClientPoolFactory implements ObjectFactory<BinaryClient> {
	
	private String 	host;
	private int 	port;
	
	public BinaryClientPoolFactory (String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	@Override
	public BinaryClient create () {
		return new NettyBinaryClient (host, port);
	}

	@Override
	public void destroy (BinaryClient client) {
		((NettyBinaryClient)client).destroy ();
	}

	@Override
	public boolean validate (BinaryClient client) {
		return true;
	}

}

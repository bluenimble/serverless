package com.bluenimble.platform.tools.binary.impls.netty;

import com.bluenimble.platform.tools.binary.BinaryClient;
import com.bluenimble.platform.tools.binary.BinaryClientFactory;
import com.bluenimble.platform.tools.binary.Callback;
import com.bluenimble.platform.tools.binary.impls.netty.pool.BinaryClientPoolFactory;
import com.bluenimble.platform.tools.binary.impls.netty.pool.ObjectPool;
import com.bluenimble.platform.tools.binary.impls.netty.pool.PoolConfig;

public class NettyBinaryClientFactory implements BinaryClientFactory {

	private static final long serialVersionUID = 5335634635854424930L;
	
	private ObjectPool<BinaryClient> pool;
	
	public NettyBinaryClientFactory (String host, int port, PoolConfig config) {
		pool = new ObjectPool<BinaryClient> (config, new BinaryClientPoolFactory (host, port));
	}
	
	@Override
	public BinaryClient create (Callback callback) {
		NettyBinaryClient client = (NettyBinaryClient)pool.borrowObject ().getObject ();
		client.useCallback (callback);
		return client;
	}

}

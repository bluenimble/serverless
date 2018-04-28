package vom.bluenimble.platform.tools.binary.tests;

import vom.bluenimble.platform.tools.binary.BinaryClientFactory;
import vom.bluenimble.platform.tools.binary.impls.netty.NettyBinaryClientFactory;
import vom.bluenimble.platform.tools.binary.impls.netty.pool.PoolConfig;

public class ClientTest {

	public static void main (String[] args) throws Exception {
		
		BinaryClientFactory factory = new NettyBinaryClientFactory (
			"localhost", 7070, 
			new PoolConfig ()
				.setPartitionSize (5)
				.setMinSize (5)
				.setMaxSize (10)
				.setMaxIdleMilliseconds(60 * 1000 * 5)
		);
		
		for (int i = 0; i < 20; i++) {
			new Worker (factory, String.valueOf (i)).start ();
		}
		
	}	
}

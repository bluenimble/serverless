/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.tools.binary.impls.netty;

import com.bluenimble.platform.pooling.ObjectPool;
import com.bluenimble.platform.pooling.PoolConfig;
import com.bluenimble.platform.tools.binary.BinaryClient;
import com.bluenimble.platform.tools.binary.BinaryClientFactory;
import com.bluenimble.platform.tools.binary.impls.netty.pool.BinaryClientPoolFactory;

public class NettyBinaryClientFactory implements BinaryClientFactory {

	private static final long serialVersionUID = 5335634635854424930L;
	
	private ObjectPool<BinaryClient> pool;
	
	public NettyBinaryClientFactory (String host, int port, PoolConfig config) {
		pool = new ObjectPool<BinaryClient> (config, new BinaryClientPoolFactory (host, port));
	}
	
	@Override
	public BinaryClient create () {
		return (NettyBinaryClient)pool.borrowObject ().getObject ();
	}

	@Override
	public void shutdown () {
		if (pool != null) {
			try {
				pool.shutdown ();
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

}

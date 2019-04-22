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
 
// REF: https://github.com/fusesource/stompjms 
package com.bluenimble.platform.cache.impls.memcached;

import java.nio.ByteBuffer;

import com.bluenimble.platform.cache.Cache;

import net.spy.memcached.MemcachedClient;

public class MemCachedCache implements Cache {

	private static final long serialVersionUID = 1782531142979435163L;
	
	private MemcachedClient client;
	
	public MemCachedCache (MemcachedClient client) {
		this.client = client;
	}

	@Override
	public void put (byte [] key, byte [] value, int ttl) {
		client.set (new String (key), ttl, value);
	}

	@Override
	public byte [] get (byte [] key, boolean remove) {
		if (remove) {
			return (byte [])client.getAndTouch (new String (key), 200).getValue ();
		} else {
			return (byte [])client.get (new String (key));
		}
	}

	@Override
	public void delete (byte [] key) {
		client.delete (new String (key));
	}

	@Override
	public long increment (byte [] key, long increment) {
		if (increment == 0) {
			return toLong (get (key, false));
		}
		if (increment > 0) {
			return client.incr (new String (key), increment);
		} else {
			return client.decr (new String (key), increment * -1);
		}
	}
	
	private long toLong (byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}

}

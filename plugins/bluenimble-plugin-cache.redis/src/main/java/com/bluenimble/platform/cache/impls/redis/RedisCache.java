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
package com.bluenimble.platform.cache.impls.redis;

import java.nio.ByteBuffer;
import java.util.Set;

import com.bluenimble.platform.cache.Cache;

import redis.clients.jedis.Jedis;

public class RedisCache implements Cache {

	private static final long serialVersionUID = 1782531142979435163L;
	
	private Jedis		 client;
	
	public RedisCache (Jedis client) {
		this.client = client;
	}

	@Override
	public Set<byte []> keys (String pattern) {
		return client.keys (pattern.getBytes ());
	}

	@Override
	public void put (byte [] key, byte [] value, int ttl) {
		if (ttl > 0) {
			client.setex (key, ttl, value);
		} else {
			client.set (key, value);
		}
	}

	@Override
	public byte [] get (byte [] key, boolean remove) {
		byte [] value = client.get (key);
		if (remove && value != null) {
			client.del (key);
		}
		return value;
	}

	@Override
	public void delete (byte [] key) {
		client.del (key);
	}

	@Override
	public long increment (byte [] key, long increment) {
		if (increment == 0) {
			return toLong (get (key, false));
		}
		if (increment > 0) {
			return client.incrBy (key, increment);
		} else {
			return client.decrBy (key, increment * -1);
		}
	}
	
	/*
	private byte[] toBytes (long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(x);
	    return buffer.array();
	}
	*/

	private long toLong (byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}

	@Override
	public void finish (boolean withError) {
		
	}

	@Override
	public void recycle () {
		client.close ();
	}

}

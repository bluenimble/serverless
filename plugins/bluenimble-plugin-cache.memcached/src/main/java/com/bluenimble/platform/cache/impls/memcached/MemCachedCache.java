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

import com.bluenimble.platform.cache.Cache;

import net.spy.memcached.MemcachedClient;

public class MemCachedCache implements Cache {

	private static final long serialVersionUID = 1782531142979435163L;
	
	private MemcachedClient client;
	
	public MemCachedCache (MemcachedClient client) {
		this.client = client;
	}

	@Override
	public void put (String key, Object value, int ttl) {
		client.set (key, ttl, value);
	}

	@Override
	public Object get (String key, boolean remove) {
		if (remove) {
			return client.getAndTouch (key, 200).getValue ();
		} else {
			return client.get (key);
		}
	}

	@Override
	public void delete (String key) {
		client.delete (key);
	}

	@Override
	public void increment (String key, int increment, long dValue, int ttl, boolean async) {
		if (increment == 0) {
			return;
		}
		if (increment < 0) {
			if (async) {
				if (ttl > 0) { 
					client.asyncDecr (key, increment, dValue, ttl);
				} else {
					client.asyncDecr (key, increment, dValue);
				}
			} else {
				if (ttl > 0) { 
					client.decr (key, increment, dValue, ttl);
				} else {
					client.decr (key, increment, dValue);
				}
			}
		} else {
			if (async) {
				if (ttl > 0) { 
					client.asyncIncr (key, increment, dValue, ttl);
				} else {
					client.asyncIncr (key, increment, dValue);
				}
			} else {
				if (ttl > 0) { 
					client.incr (key, increment, dValue, ttl);
				} else {
					client.incr (key, increment, dValue);
				}
			}
		}
	}

}

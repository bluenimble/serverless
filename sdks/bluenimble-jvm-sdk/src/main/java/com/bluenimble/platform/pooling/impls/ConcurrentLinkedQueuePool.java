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
package com.bluenimble.platform.pooling.impls;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.bluenimble.platform.pooling.PoolException;
import com.bluenimble.platform.pooling.PoolSettings;
import com.bluenimble.platform.pooling.PoolableObject;

public class ConcurrentLinkedQueuePool<T> extends AbstractPool<T> {

	public ConcurrentLinkedQueuePool(final PoolableObject<T> poolableObject, final PoolSettings<T> settings) throws PoolException {
		super(poolableObject, settings);
		queue = new ConcurrentLinkedQueue<T>();
		init ();
	}

	@Override
	public T getObj() throws PoolException {
		T t = queue.poll();
		if (t==null) {
			t = poolableObject.make();
			totalSize.incrementAndGet();
		}
		poolableObject.activate(t);
		return t;
	}

}

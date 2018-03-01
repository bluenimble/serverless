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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.bluenimble.platform.pooling.PoolException;
import com.bluenimble.platform.pooling.PoolSettings;
import com.bluenimble.platform.pooling.PoolableObject;

public abstract class BlockingQueueObjectPool<T> extends AbstractPool<T> {
	
	protected LinkedBlockingQueue<T> linkQueue;
	
	public BlockingQueueObjectPool (final PoolableObject<T> poolableObject, final PoolSettings<T> settings) throws PoolException {
		super(poolableObject,settings);
		queue = new LinkedBlockingQueue<T>();
		linkQueue = (LinkedBlockingQueue<T>) queue;
		init ();
	}


	@Override
	public T getObj () throws PoolException {
		if (queue.size() == 0 && totalSize.get() < settings.max()) {
			create ();
		}
		T t = null;
		try {
			t = linkQueue.poll (settings.maxWait (), TimeUnit.SECONDS);
			poolableObject.activate (t);
		} catch (InterruptedException e) {
			throw new PoolException (e);
		}
		
		return t;
	}

}

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

import com.bluenimble.platform.pooling.ObjectPool;
import com.bluenimble.platform.pooling.PoolException;
import com.bluenimble.platform.pooling.PoolSettings;
import com.bluenimble.platform.pooling.PoolableObject;

public class PoolFactory<T> {
	final PoolSettings<T> settings;

	protected AbstractPool<T> pool;
	protected final PoolableObject<T> poolableObject;

	public PoolFactory (PoolSettings<T> settings, PoolableObject<T> poolableObject) {
		this.settings = settings;
		this.poolableObject = poolableObject;
	}

	public ObjectPool<T> getPool () throws PoolException {
		if (pool == null) {
			createPoolInstance ();
		}
		return pool;
	}
	
	public void clear () {
		if (pool != null && pool instanceof Controlable) {
			((Controlable) pool).clear();
		}
	}

	private static class BBObjectPool<T> extends BlockingQueueObjectPool<T> {

		public BBObjectPool (PoolableObject<T> poolableObject, PoolSettings<T> settings) throws PoolException {
			super (poolableObject, settings);
		}

	}

	private synchronized void createPoolInstance () throws PoolException {
		if (pool == null) {
			if (settings.max () > 0) {
				pool = new BBObjectPool<T> (poolableObject, settings);
			} else {
				pool = new ConcurrentLinkedQueuePool<T> (poolableObject, settings);
			}
		}
	}

	public PoolSettings<T> settings () {
		return settings;
	}

}
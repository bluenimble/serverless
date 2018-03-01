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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import com.bluenimble.platform.pooling.ObjectPool;
import com.bluenimble.platform.pooling.PoolException;
import com.bluenimble.platform.pooling.PoolSettings;
import com.bluenimble.platform.pooling.PoolableObject;

public abstract class AbstractPool<T> implements ObjectPool<T>, Controlable {
	final PoolSettings<T> settings;
	final PoolableObject<T> poolableObject;
	Queue<T> queue;
	final AtomicInteger totalSize = new AtomicInteger(0);

	public AbstractPool (final PoolableObject<T> poolableObject, final PoolSettings<T> settings) {
		this.poolableObject = poolableObject;
		this.settings = settings;

	}
	protected void init () throws PoolException {
		for (int n = 0; n < settings.min(); n++) {
			create ();
		}
	}

	protected void create () throws PoolException {
		T t = poolableObject.make ();
		totalSize.incrementAndGet ();
		queue.add (t);
	}


	@Override
	public void returnObj (final T t) {
		if (t == null)
			return;
		
		if (!settings.validateWhenReturn () || poolableObject.validate (t)) {
			poolableObject.passivate (t);
			queue.add (t);
		} else {
			destroyObject (t);
		}

	}

	private void destroyObject(final T t) {
		poolableObject.destroy (t);
		totalSize.decrementAndGet ();
	}

	@Override
	public int idles() {
		return queue.size();
	}

	@Override
	public void remove (int nbObjects) {
		for (int n = 0; n < nbObjects; n++) {
			T t = queue.poll();
			if (t == null) {
				break;
			}
			destroyObject(t);

		}

	}

	@Override
	public void clear () {
		for (; queue.size () > 0;) {
			T t = queue.poll ();
			destroyObject (t);
			
		}
		totalSize.set (0);
	}

	@Override
	public void destroy() {
		clear ();
	}

	@Override
	public int actives() {
		return totalSize.get() - queue.size();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		int total = totalSize.get();
		b.append(this.getClass().getSimpleName());
		b.append(",  totalSize: ").append(total);
		b.append(", numActive: ").append(actives());
		b.append(", numIdle: ").append(idles());
		b.append(", max: ").append(settings.max());
		b.append(", queueSize: ").append(queue.size());
		return b.toString();
	}

	@Override
	public void validateIdles() {
		List<T> listT = new ArrayList<T>(queue.size());
		int queueSise = queue.size();

		for (int n = 0; n < queueSise; n++) {
			T t = queue.poll();
			if (t == null)
				break;
			if (poolableObject.validate(t)) {
				listT.add(t);
			} else {
				destroyObject(t);
			}

		}

		for (T t : listT) {
			queue.add(t);
		}
		
		int objectsToCreate = settings.min () - totalSize.get ();
		for (int i = 0; i < objectsToCreate; i++) {
			try {
				create ();
			} catch (Exception e) {
				System.out.println ("Create object error " + e.getClass ().getSimpleName () + " " + e.getMessage ());
			}
		}

	}

}

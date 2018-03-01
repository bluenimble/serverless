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

public class PoolController<T> extends Thread {
	
	private boolean shutdown;
	
	protected PoolSettings<T> settings;

	public PoolController (PoolSettings<T> settings) {
		setName ("PoolControler");
		this.settings = settings;
		start ();
	}

	public void shutdown () {
		ObjectPool<?> pool = null;
		try {
			pool = settings.pool ();
		} catch (PoolException e) {
			e.printStackTrace ();
			return;
		}
		if (pool instanceof Controlable) {
			Controlable controlable = (Controlable) pool;
			controlable.destroy ();
		}
		shutdown = true;
	}
	
	@Override
	public void run () {
		System.out.println ("Starting " + getName ());
		while (!shutdown) {
			try {
				sleep (PoolSettings.timeBetweenTwoControls () * 1000);
				checkPool ();
			} catch (InterruptedException e) {
				System.out.println ("PoolControler " + e.getMessage ());
			}
		}
		System.out.println ("PoolControler received shutdown signal ");
	}

	/**
	 * Remove idle <br>
	 * Validate idle
	 * 
	 * 
	 */
	private synchronized void checkPool () {
		ObjectPool<?> pool;
		try {
			pool = settings.pool ();
		} catch (PoolException e) {
			e.printStackTrace ();
			return;
		}
		if (pool instanceof Controlable) {
			Controlable controlable = (Controlable) pool;
			/*
			 * Remove idle
			 */
			int idleToRemoves = controlable.idles() - settings.maxIdle();
			if (idleToRemoves > 0) {
				controlable.remove(idleToRemoves);
			}
			/*
			 * Check idle
			 */
			controlable.validateIdles();

		}
	}

}

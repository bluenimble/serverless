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
package com.bluenimble.platform.pooling.tests;

import com.bluenimble.platform.pooling.ObjectPool;
import com.bluenimble.platform.pooling.PoolException;
import com.bluenimble.platform.pooling.PoolSettings;
import com.bluenimble.platform.pooling.PoolableObjectBase;

public class TestPooling {
	
	private static ObjectPool<StringBuilder> objectPool;
	
	public static void main (String [] args) throws PoolException {
		// Create your PoolSettings with an instance of PoolableObject
		PoolSettings<StringBuilder> poolSettings = new PoolSettings<StringBuilder>(
            new PoolableObjectBase<StringBuilder>() {
                @Override
                public StringBuilder make () {
                	return new StringBuilder ();
                }
                @Override
                public void activate (StringBuilder t) {
                	t.setLength (0);
                }
            });
		// Add some settings
		poolSettings.min (10).max (25).maxWait (5);

		objectPool = poolSettings.pool ();
	
		use (100);

        poolSettings.shutdown ();

	}
	
	private static void use (int size) {
		for (int i = 0; i < size; i++) {
			thread ();
		}
	}
	
	private static void thread () {
		
		new Thread () {
			public void run () {
				// Use your pool
				StringBuilder buffer = null;
				try {
			        buffer = objectPool.getObj ();
			        // Do something with your object
			        buffer.append ("yyyy");
			        try {
			        	sleep (3000);
					} catch (InterruptedException e) {
						System.out.println ("PoolControler " + e.getMessage ());
					}
			        System.out.println (buffer);
				} catch (PoolException e) {
			        e.printStackTrace ();
				} finally {
			        // Don't forget to return object to the pool
			        objectPool.returnObj (buffer);
				}
			}
		}.start();
	
	}
	
}

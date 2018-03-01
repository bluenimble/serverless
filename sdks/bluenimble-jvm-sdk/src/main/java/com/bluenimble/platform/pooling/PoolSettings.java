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
package com.bluenimble.platform.pooling;

import com.bluenimble.platform.pooling.impls.PoolController;
import com.bluenimble.platform.pooling.impls.PoolFactory;

public class PoolSettings<T> {
	/**
	 * Wait (in seconds) 
	 */
	public static final int DEFAUL_MAX_WAIT = 5;
	public static final int DEFAULT_MIN = 10;
	public static final int DEFAULT_MAX = 30;
	/**
	 * Control thread 
	 */
	public static final int DEFAULT_TIME_BETWEEN_CONTROLS = 30;
	
	private static 		int TIME_BETWEEN_CONTROLS = DEFAULT_TIME_BETWEEN_CONTROLS;

	private int maxWait = DEFAUL_MAX_WAIT;
	private int min = DEFAULT_MIN;
	private int max = DEFAULT_MAX;
	private int maxIdle = min;
	private boolean validateWhenReturn = false;
	
	private final PoolFactory<T> poolFactory;
	private PoolController<T> poolController;
	
	public PoolSettings (final PoolableObject<T> poolableObject) {
		this.poolFactory = new PoolFactory<T>(this, poolableObject);
		this.poolController = new PoolController<T> (this);
	}

	public ObjectPool<T> pool () throws PoolException {
		return poolFactory.getPool ();
	}

	public PoolSettings<T> maxIdle (final int maxIdle) {
		this.maxIdle = maxIdle < min ? min : maxIdle;
		return this;
	}

	public int maxIdle () {
		return this.maxIdle;
	}

	public PoolSettings<T> maxWait (final int maxWait) {
		this.maxWait = maxWait;
		return this;
	}

	public PoolSettings<T> min (final int min) {
		this.min = min;
		maxIdle = min;
		if (max > 0 && min > max) {
			max (min);
		}
		return this;
	}

	public PoolSettings<T> max (final int max) {
		this.max = max;
		if (max > 0 && max < min) {
			min (max);
		}
		return this;
	}

	public int min () {
		return min;
	}

	public int maxWait () {
		return maxWait;
	}

	public int max () {
		return max;
	}

	public void shutdown () {
		poolController.shutdown ();
	}
	
	/**
	 * if true invoke PoolableObject.validate() method
	 * @param validateWhenReturn
	 * @return
	 */
	public PoolSettings<T> validateWhenReturn (boolean validateWhenReturn) {
		this.validateWhenReturn = validateWhenReturn;
		return this;
	}

	public boolean validateWhenReturn () {
		return validateWhenReturn;
	}
	
	public static void timeBetweenTwoControls (int time) {
		TIME_BETWEEN_CONTROLS = time;
	}
	
	public static int timeBetweenTwoControls () {
		return TIME_BETWEEN_CONTROLS;
	}

	public void clearPool () {
		poolFactory.clear ();
	}
	
}
 
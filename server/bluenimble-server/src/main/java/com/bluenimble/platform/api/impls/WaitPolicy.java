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
package com.bluenimble.platform.api.impls;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WaitPolicy implements RejectedExecutionHandler {

	private long time;
	private TimeUnit timeUnit;

	public WaitPolicy () {
		this (Long.MAX_VALUE, TimeUnit.SECONDS);
	}

	public WaitPolicy (long time, TimeUnit timeUnit) {
		super ();
		this.time = (time < 0 ? Long.MAX_VALUE : time);
		this.timeUnit = timeUnit;
	}

	public void rejectedExecution (Runnable r, ThreadPoolExecutor e) {
		try {
			if (e.isShutdown() || !e.getQueue ().offer (r, time, timeUnit)) {
				throw new RejectedExecutionException ();
			}
		} catch (InterruptedException ie) {
			Thread.currentThread ().interrupt ();
			throw new RejectedExecutionException (ie);
		}
	}

}
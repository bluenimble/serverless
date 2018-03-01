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
package com.bluenimble.platform.server.tracking.impls.database;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.server.tracking.ServerRequestTrack;
import com.bluenimble.platform.server.tracking.ServerRequestTracker;

public class DatabaseApiRequestTracker implements ServerRequestTracker {

	private static final long serialVersionUID = 393416228007740651L;
	
	protected int size 			= 20;
	protected int keepAliveTime = 5;
	protected int capacity 		= 100;
	
	private ThreadGroup threadGroup = new ThreadGroup ("RequestTracker");
	
	private ApiContext	context 	= new ApiContext () {
		private static final long serialVersionUID = 3571684809770314117L;
	
		@Override
		public void addRecyclable (String name, Recyclable r) {
		}
		@Override
		public Recyclable getRecyclable (String name) {
			return null;
		}
		@Override
		public void recycle () {
		}
		@Override
		public Object getReference () {
			return null;
		}
		@Override
		public void setReference (Object object) {
			
		}
		
	};
	
	private TrackingExecutor executor;

	public DatabaseApiRequestTracker (int size) {
		threadGroup.setMaxPriority (Thread.MIN_PRIORITY);
		this.size = size;
		this.capacity = 2 * this.size;
		executor = new TrackingExecutor ();
	}
	
	class TrackingExecutor extends ThreadPoolExecutor {
		public TrackingExecutor () {
			super (size, size, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable> (capacity), new ThreadFactory () {
				@Override
				public Thread newThread (Runnable r) {
					return new Thread (threadGroup, r);
				}
			});
		}
	}
	
	@Override
	public ServerRequestTrack create (Api api, ApiRequest request) {
		return new DatabaseApiRequestTrack (this, api, request);
	}
	
	public int getSize () {
		return size;
	}
	public void setSize (int size) {
		this.size = size;
	}

	public int getKeepAliveTime () {
		return keepAliveTime;
	}
	public void setKeepAliveTime (int keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}

	public int getCapacity () {
		return capacity;
	}
	public void setCapacity (int capacity) {
		this.capacity = capacity;
	}

	TrackingExecutor executor () {
		return executor;
	}
	
	ApiContext context () {
		return context;
	}

}

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
package com.bluenimble.platform.server.tracking.impls.indexer;

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

public class IndexerApiRequestTracker implements ServerRequestTracker {

	private static final long serialVersionUID = 393416228007740651L;
	
	protected int capacity 		= 100;
	protected int minThreads 	= 5;
	protected int maxThreads 	= 10;
	protected int keepAliveTime = 30;
	
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
		public void finish () {
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

	public IndexerApiRequestTracker (int capacity, int minThreads, int maxThreads, int keepAliveTime) {
		threadGroup.setMaxPriority (Thread.MIN_PRIORITY);
		this.capacity 		= capacity;
		this.minThreads 	= minThreads;
		this.maxThreads 	= maxThreads;
		this.keepAliveTime 	= keepAliveTime;
		
		executor = new TrackingExecutor ();
	}
	
	class TrackingExecutor extends ThreadPoolExecutor {
		public TrackingExecutor () {
			super (minThreads, maxThreads, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable> (capacity), new ThreadFactory () {
				@Override
				public Thread newThread (Runnable r) {
					return new Thread (threadGroup, r);
				}
			});
		}
	}
	
	@Override
	public ServerRequestTrack create (Api api, ApiRequest request) {
		return new IndexerApiRequestTrack (this, api, request);
	}

	TrackingExecutor executor () {
		return executor;
	}
	
	ApiContext context () {
		return context;
	}

}

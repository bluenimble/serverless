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

import java.lang.Thread.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.CodeExecutor;
import com.bluenimble.platform.api.CodeExecutorException;
import com.bluenimble.platform.api.impls.ApiSpaceImpl.Describe;
import com.bluenimble.platform.json.JsonObject;

public class DefaultCodeExecutor implements CodeExecutor {

	private static final long serialVersionUID 	= -9133400554882567946L;
	
	private static final String StateAvailable 	= "available";
	
	private static final String DefaultGroup 	= "DefaultGroup";
	private static final String DefaultPriority = "norm";
	
	private static Map<String, Integer> Priorities = new HashMap<String, Integer> ();
	static {
		Priorities.put ("max", 				Thread.MAX_PRIORITY);
		Priorities.put ("min", 				Thread.MIN_PRIORITY);
		Priorities.put (DefaultPriority, 	Thread.NORM_PRIORITY);
	}
	
	interface Spec {
		String Group 		= "group";
		String Priority 	= "priority";

		String CoreSize 	= "coreSize";
		String MaxSize 		= "maxSize";
		String KeepAlive 	= "keepAlive";
		String Queue 		= "queue";
		String Timeout 		= "timeout";
		String AwaitTermination
							= "awaitTermination";
	}
	
	protected JsonObject _service;
	
	private static final Future<Void> 	NoFuture = new Future<Void> () {
		@Override
		public boolean cancel (boolean mayInterruptIfRunning) {
			return false;
		}
		@Override
		public boolean isCancelled () {
			return false;
		}
		@Override
		public boolean isDone () {
			return false;
		}
		@Override
		public Void get () throws InterruptedException, ExecutionException {
			return null;
		}
		@Override
		public Void get (long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	};
	
	protected ThreadGroup		group;
	protected ExecutorService 	service;
	
	protected long				timeout;
	protected long				awaitTermination;
	
	@Override
	public Future<Void> execute (Callable<Void> callable, Mode mode) throws CodeExecutorException {
		
		if (callable == null) {
			return NoFuture;
		}
		
		if (service == null) {
			mode = Mode.Sync;
		}
		
		if (mode.equals (Mode.Sync)) {
			try {
				callable.call ();
			} catch (Exception e) {
				throw new CodeExecutorException (e.getMessage (), e);
			}
			return NoFuture;
		}
		
		Future<Void> future = service.submit (callable);
		
		if (mode.equals (Mode.AsyncWait)) {
			try {
				future.get (timeout, TimeUnit.MILLISECONDS);
	   		} catch (TimeoutException e) {
	   			future.cancel (true);
	            throw new CodeExecutorException (e.getMessage (), e);
			} catch (Exception e) {
	            throw new CodeExecutorException (e.getMessage (), e);
			} 
		}
		return future;
		
	}

	@Override
	public JsonObject describe () {
		if (service == null) {
			return null;
		}
		JsonObject oThreads = new JsonObject ();
		
		Thread [] threads = listThreads ();
		
		if (threads == null) {
			return null;
		}

		for (Thread t : threads) {
			if (t == null) {
				continue;
			}
			
			String status = t.getState ().name ();
			if (State.WAITING.equals (t.getState ())) {
				status = StateAvailable;
			}
			
			JsonObject oth = (JsonObject)new JsonObject ().set (Describe.Worker.Id, t.getId ()).set (Describe.Worker.Name, t.getName ()).set (Describe.Worker.Status, status);
			if (t instanceof SpaceThread) {
				SpaceThread st = (SpaceThread)t;
				ApiRequest request = st.getRequest ();
				if (request != null) {
					JsonObject oRequest = new JsonObject ();
					oRequest.set (ApiRequest.Fields.Id, request.getId ());
					oRequest.set (ApiRequest.Fields.Verb, request.getVerb ().name ());
					oRequest.set (ApiRequest.Fields.Endpoint, request.getEndpoint ());
					oRequest.set (ApiRequest.Fields.Timestamp, Lang.toUTC (request.getTimestamp ()));
					
					oth.set (Describe.Worker.request.class.getSimpleName (), oRequest);
					
					if (request.getService () != null) {
						JsonObject oService = new JsonObject ();
						String script = Json.getString (request.getService ().getSpiDef (), Api.Spec.Spi.Function);
						oService.set (Api.Spec.Spi.Function, script);
						if (script == null) {
							oService.set (Api.Spec.Spi.Function, request.getService ().getSpi ().getClass ().getSimpleName ());
						}
						oService.set (ApiService.Spec.Endpoint, request.getService ().getEndpoint ());
						
						oth.set (Describe.Worker.Service, oService);
					}
					
				}
			}
			oThreads.set (t.getName (), oth);
		}
		return oThreads;
	}
	
	@Override
	public boolean interrupt (long worker) {
		Thread [] threads = listThreads ();
		if (threads == null) {
			return false;
		}
		for (Thread t : threads) {
			if (worker == t.getId ()) {
				t.interrupt ();
				return true;
			}
		}		
		return false;
	}

	@Override
	public CodeExecutor start () {
		if (Json.isNullOrEmpty (_service)) {
			return this;
		}
		
		group = new ThreadGroup (Json.getString (_service, Spec.Group, DefaultGroup));
	
		String sPriority = Json.getString (_service, Spec.Priority, DefaultPriority).toLowerCase ();
		
		Integer priority = Priorities.get (sPriority);
		if (priority == null) {
			priority = Thread.NORM_PRIORITY;
		}
		group.setMaxPriority (priority);

		service = new ThreadPoolExecutor (
			Json.getInteger (_service, Spec.CoreSize, 10), Json.getInteger (_service, Spec.MaxSize, 10),
			Json.getLong (_service, Spec.KeepAlive, 0L), TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable> (Json.getInteger (_service, Spec.Queue, 10)), 
			new SpaceThreadFactory (group)
		);
		
		timeout 			= Json.getLong (_service, Spec.Timeout, 600000);
		awaitTermination 	= Json.getLong (_service, Spec.AwaitTermination, 60000);
		
		return this;
		
	}
	
	@Override
	public void shutdown () {
		if (service == null) {
			return;
		}
		
		service.shutdown (); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!service.awaitTermination (awaitTermination, TimeUnit.MILLISECONDS)) {
				service.shutdownNow (); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!service.awaitTermination (awaitTermination, TimeUnit.MILLISECONDS)) {
					System.err.println ("Service did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			service.shutdownNow ();
			// Preserve interrupt status
			Thread.currentThread ().interrupt ();
		}
	}

	public void setService (JsonObject _service) {
		this._service = _service;
	}

	public JsonObject getService () {
		return _service;
	}

	private Thread [] listThreads () {
		if (group == null) {
			return null;
		}
		Thread [] threads = new Thread [group.activeCount ()];
		group.enumerate (threads, false);
		return threads;
	}

}

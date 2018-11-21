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

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiStatus;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.StatusManager;
import com.bluenimble.platform.server.utils.ConfigKeys;

public class DefaultStatusManager implements StatusManager {

	private static final long serialVersionUID = 5491648804043859460L;
	
	private static final String DeletedStatus 		= "_Deleted_";
	
	interface Spec {
		String Status 	= "status";
		String Services = "services";
	}
	
	class Change {
		String api;
		String service;
		String status;
		Change (String api, String status) { this.api = api; this.status = status; }
		Change (String api, String service, String status) { this (api, status); this.service = service; }
	}
	
	private BlockingQueue<Change> 		changes = new LinkedBlockingQueue<Change> ();
	
	private long						delay 	= 0;
	private long						period 	= 20;
	private boolean						readOnly;
	
	private ScheduledExecutorService 	listener;
	
	private File 						statusFile;

	private JsonObject 					status;
	
	private Tracer						tracer;
	
	public DefaultStatusManager () throws Exception {
	}
	
	@Override
	public void init (ApiSpace aSpace) throws Exception {
		
		ApiSpaceImpl space = (ApiSpaceImpl)aSpace;
		
		tracer = space.tracer ();
		
		File statusFile = new File (space.home, ConfigKeys.StatusFile);
		
		if (!statusFile.exists () && space.server.tenant () != null) {
			statusFile = new File (space.server.tenant (), space.getNamespace () + Lang.SLASH + ConfigKeys.StatusFile);
		}
		
		if (!statusFile.exists ()) {
			status = new JsonObject ();
			if (!statusFile.getParentFile ().exists ()) {
				statusFile.getParentFile ().mkdirs ();
			}
			Json.store (status, statusFile);
		}

		this.statusFile = statusFile;
		
		if (status == null) {
			status = Json.load (statusFile);
		}
		
		start ();
	}

	public void start () {
		if (readOnly) {
			return;
		}
		listener = Executors.newSingleThreadScheduledExecutor ();
		listener.scheduleAtFixedRate (new Runnable () {
			@Override
			public void run () {
				Change change = null;
				try {
					while ((change = changes.poll (3, TimeUnit.SECONDS)) != null) {
						_update (change);
					}
				} catch (InterruptedException e) {
				}
				try {
					if (change != null && !readOnly) {
						Json.store (status, statusFile);
					}
				} catch (Exception e) {
					tracer.log (Tracer.Level.Error, Lang.BLANK, e);
				}
			}
		}, delay, period, TimeUnit.SECONDS);
	}

	@Override
	public ApiStatus get (Api api) {
		if (status == null) {
			return null;
		}
		
		JsonObject oApi = Json.getObject (status, api.getNamespace ());
		if (Json.isNullOrEmpty (oApi)) {
			return null;
		}
		
		String sStatus = Json.getString (oApi, Spec.Status);
		if (Lang.isNullOrEmpty (sStatus)) {
			return null;
		}
		
		ApiStatus apiStatus = null;
		try {
			apiStatus = ApiStatus.valueOf (sStatus);
		} catch (Exception ex) {
			// ignore
		}
		
		return apiStatus;
	}

	@Override
	public ApiStatus get (Api api, ApiService service) {
		String sStatus = (String)Json.find (status, api.getNamespace (), Spec.Services, service.getVerb ().name () + Json.getString (service.toJson (), ApiService.Spec.Endpoint));
		if (Lang.isNullOrEmpty (sStatus)) {
			return null;
		}
		
		ApiStatus status = null;
		try {
			status = ApiStatus.valueOf (sStatus);
		} catch (Exception ex) {
			// ignore
		}
		return status;
	}

	@Override
	public void update (Api api, ApiStatus status) {
		Change change = new Change (api.getNamespace (), status.name ());
		if (readOnly) {
			_update (change);
		} else {
			changes.offer (change);
		}
	}

	@Override
	public void update (Api api, ApiService service, ApiStatus status) {
		Change change = new Change (
			api.getNamespace (), 
			service.getVerb ().name () + Json.getString (service.toJson (), ApiService.Spec.Endpoint), 
			status.name ()
		);
		if (readOnly) {
			_update (change);
		} else {
			changes.offer (change);
		}
	}

	@Override
	public void delete (Api api) {
		Change change = new Change (api.getNamespace (), DeletedStatus);
		if (readOnly) {
			_update (change);
		} else {
			changes.offer (change);
		}
	}

	@Override
	public void delete (Api api, ApiService service) {
		Change change = new Change (
			api.getNamespace (), 
			service.getVerb ().name () + Json.getString (service.toJson (), ApiService.Spec.Endpoint), 
			DeletedStatus
		);
		if (readOnly) {
			_update (change);
		} else {
			changes.offer (change);
		}
	}
	
	public long getDelay () {
		return delay;
	}
	public void setDelay (long delay) {
		this.delay = delay;
	}

	public long getPeriod () {
		return period;
	}
	public void setPeriod (long period) {
		this.period = period;
	}

	public boolean isReadOnly () {
		return readOnly;
	}
	public void setReadOnly (boolean readOnly) {
		this.readOnly = readOnly;
	}

	private void _update (Change change) {
		if (change == null) {
			return;
		}
		JsonObject oApi = Json.getObject (status, change.api);
		JsonObject services = Json.getObject (oApi, Spec.Services);
		
		if (DeletedStatus.equals (change.status)) {
			if (change.service == null) {
				status.remove (change.api);
			} else if (services != null) {
				services.remove (change.service);
			}
			return;
		}
		
		if (oApi == null) {
			oApi = new JsonObject ();
			status.set (change.api, oApi);
		}
		
		if (change.service != null) {
			if (services == null) {
				services = new JsonObject ();
				oApi.set (Spec.Services, services);
			}
			services.set (change.service, change.status);
		} else {
			oApi.set (Spec.Status, change.status);
		}
	}

}

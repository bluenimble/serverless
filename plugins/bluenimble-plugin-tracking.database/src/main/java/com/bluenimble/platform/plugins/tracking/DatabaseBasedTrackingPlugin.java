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
package com.bluenimble.platform.plugins.tracking;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.tracking.impls.database.DatabaseApiRequestTracker;

public class DatabaseBasedTrackingPlugin extends AbstractPlugin {

	private static final long serialVersionUID = -7715328225346939289L;
	
	interface Spec {
		String Name 			= "name";
		String Capacity 		= "capacity";
		String MinThreads 		= "minThreads";
		String MaxThreads 		= "maxThreads";
		String KeepAliveTime 	= "keepAliveTime";
	}
	
	private JsonObject tracker;

	@Override
	public void init (ApiServer server) throws Exception {
		DatabaseApiRequestTracker rTracker = new DatabaseApiRequestTracker (
			Json.getInteger (tracker, Spec.Capacity, 100),
			Json.getInteger (tracker, Spec.MinThreads, 5),
			Json.getInteger (tracker, Spec.MaxThreads, 10),
			Json.getInteger (tracker, Spec.KeepAliveTime, 30)
		);
		server.addRequestTracker (Json.getString (tracker, Spec.Name), rTracker);
	}

	public JsonObject getTracker () {
		return tracker;
	}

	public void setTracker (JsonObject tracker) {
		this.tracker = tracker;
	}

}

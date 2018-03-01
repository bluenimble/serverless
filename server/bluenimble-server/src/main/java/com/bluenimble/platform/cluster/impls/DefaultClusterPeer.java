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
package com.bluenimble.platform.cluster.impls;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.cluster.ClusterMessage;
import com.bluenimble.platform.cluster.ClusterPeer;
import com.bluenimble.platform.json.JsonObject;

public class DefaultClusterPeer implements ClusterPeer {

	private static final long serialVersionUID = -8432132213545304705L;

	private static final String Id = "id";
	
	private JsonObject source = new JsonObject ();
	
	public DefaultClusterPeer (String id) {
		source.set (Id, id);
	}
	
	@Override
	public String id () {
		return Json.getString (source, Id);
	}

	@Override
	public JsonObject describe () {
		return source;
	}

	@Override
	public void send (ClusterMessage message) {
		
	}

	@Override
	public void disconnect () {
	}

}

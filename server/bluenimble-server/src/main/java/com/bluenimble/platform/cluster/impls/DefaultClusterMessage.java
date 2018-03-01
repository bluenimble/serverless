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

import com.bluenimble.platform.cluster.ClusterMessage;
import com.bluenimble.platform.cluster.ClusterSerializer;
import com.bluenimble.platform.cluster.ClusterTask;
import com.bluenimble.platform.server.ApiServer;

public class DefaultClusterMessage implements ClusterMessage {

	private static final long serialVersionUID = -3965673782050415189L;

	private Object 				object;
	private ClusterSerializer 	serializer;
	private ClusterTask 		task;
	
	public DefaultClusterMessage (ApiServer server, Object object, String serializer, String task) {
		this.object 		= object;
		this.serializer 	= server.getSerializer (serializer);
		this.task 			= server.getTask (task);
	}
	
	@Override
	public Object object () {
		return object;
	}

	@Override
	public ClusterSerializer serializer () {
		return serializer;
	}

	@Override
	public ClusterTask task () {
		return task;
	}

}

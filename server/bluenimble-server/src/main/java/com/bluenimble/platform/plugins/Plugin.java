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
package com.bluenimble.platform.plugins;

import java.io.File;

import com.bluenimble.platform.Traceable;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.ApiServer;

public interface Plugin extends Traceable {

	File 	getHome 		();
	void 	setHome 		(File home);
	
	void 	init 			(ApiServer server) throws Exception;
	
	void 	setName 		(String name);
	String 	getName 		();
	
	void 	setTitle 		(String title);
	String 	getTitle 		();
	
	void 	setDescription 	(String description);
	String 	getDescription 	();
	
	String 	getVersion 		();

	void 	kill 			();
	
	boolean isAsync 		();
	
	boolean isClosable 		();
	
	boolean isIsolated		();
	
	boolean isInitOnInstall	();
	
	void	onEvent 		(ApiServer.Event event, Object target) throws PluginRegistryException;
	
	JsonObject
			getVendor		();
	
	void	setTracer 		(Tracer tracer);
	
}

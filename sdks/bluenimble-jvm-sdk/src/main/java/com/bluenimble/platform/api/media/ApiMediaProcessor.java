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
package com.bluenimble.platform.api.media;

import java.io.Serializable;

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiMediaException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.security.ApiConsumer;

public interface ApiMediaProcessor extends Serializable {
	
	String Any = "*/*";

	void process 		(Api api, ApiService service, ApiConsumer consumer, ApiOutput output, ApiRequest request, ApiResponse response) 
							throws ApiMediaException;
	
	void addWriter 		(String name, DataWriter writer);
	void removeWriter 	(String name);
	
}

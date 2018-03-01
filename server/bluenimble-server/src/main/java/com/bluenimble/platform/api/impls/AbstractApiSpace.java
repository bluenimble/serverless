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
import java.io.IOException;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.SpaceKeyStore;
import com.bluenimble.platform.server.utils.ConfigKeys;

public abstract class AbstractApiSpace implements ApiSpace {
	
	private static final long serialVersionUID = 7855906738764532994L;
	
	protected File 			home;
	protected SpaceKeyStore keystore;
	
	protected JsonObject 	descriptor;
	
	public void saveDescriptor () throws IOException {
		Json.store (descriptor, new File (home, ConfigKeys.Descriptor.Space));
	}
	
	public JsonObject getDescriptor () {
		return descriptor;
	}
	
	public File home () {
		return home;
	}
	
	@Override
	public SpaceKeyStore keystore () {
		return keystore;
	}
	
}

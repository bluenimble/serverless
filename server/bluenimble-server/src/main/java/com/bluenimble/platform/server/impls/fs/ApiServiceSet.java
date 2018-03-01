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
package com.bluenimble.platform.server.impls.fs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.json.JsonArray;

public class ApiServiceSet implements Serializable {

	private static final long serialVersionUID = 5362451776398906247L;
	
	private Map<String, ApiService> services = new HashMap<String, ApiService> ();
	
	public ApiServiceSet () {
	}
	
	public ApiService get (String endpoint) {
		return services.get (endpoint);
	}
	
	public void remove (String endpoint) {
		services.remove (endpoint);
	}
	
	public void add (ApiService service) {
		services.put (service.getEndpoint (), service);
	}
	
	public boolean isEmpty () {
		return services.isEmpty ();
	}
	
	public void clear () {
		services.clear ();
	}
	
	public Iterator<String> endpoints () {
		if (services.isEmpty ()) {
			return null;
		}
		return services.keySet ().iterator ();
	}
	
	public void toJson (JsonArray arr) {
		Iterator<String> endpoints = endpoints ();
		if (endpoints == null) {
			return;
		}
		while (endpoints.hasNext ()) {
			arr.add (services.get (endpoints.next ()).toJson ().duplicate ());
		} 
	}
	
}

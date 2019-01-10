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

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequestVisitor;
import com.bluenimble.platform.json.JsonObject;

public class SelectiveApiRequestVisitor extends AbstractApiRequestVisitor {

	private static final long serialVersionUID = 1782406079539122227L;
	
	private static final ApiRequestVisitor Fallback = new DefaultApiRequestVisitor ();
	
	public enum Target {
		space,
		api,
		resource
	}

	public enum Placeholder {
		endpoint,
		path
	}

	protected interface Spec {
		String Static 		= "static";
		String Value 		= "value";
		String Placeholder 	= "placeholder";
		String Index 		= "index";
		String Mapping 		= "mapping";
	}

	@Override
	public void visit (AbstractApiRequest request) {
		
		// fallback
		if (spec == null) {
			Fallback.visit (request);
			return;
		}
		
		String endpoint = request.getEndpoint 	();
		String path 	= request.getPath 		();
		
		String altPath 	= (String)Json.find (spec, Spec.Static, path);
		if (!Lang.isNullOrEmpty (altPath)) {
			path = altPath;
		}
		
		if (path != null && path.startsWith (Lang.SLASH)) {
			path = path.substring (1);
		}
		if (path != null && path.endsWith (Lang.SLASH)) {
			path = path.substring (0, path.length () - 1);
		}
		if (Lang.isNullOrEmpty (path)) {
			path = null;
		}
		
		String [] aEndpoint = Lang.split (endpoint, Lang.DOT);
		String [] aPath 	= Lang.split (path, Lang.SLASH);

		// any rewrite defined at the node level
		aEndpoint 	= endpoint (request, aEndpoint);
		aPath 		= path (request, aPath);
		
		// resolve space, api and resource
		set (Target.space, 		request, aEndpoint, aPath, 0);

		// any rewrite defined at the space level? after resolving the space ns
		aEndpoint 	= endpoint (request, aEndpoint);
		aPath 		= path (request, aPath);
		
		// if response provided by any mean (in rewrite for example) or it's a bypass, return
		if (request.get (ApiRequest.Interceptors.Response) != null || 
			request.get (ApiRequest.Interceptors.Bypass) != null) {
			return;
		}
		
		set (Target.api, 		request, aEndpoint, aPath, 1);
		set (Target.resource, 	request, aEndpoint, aPath, 2);
		
		// set device
		JsonObject device = request.getDevice ();
		if (device == null) {
			device = new JsonObject ();
		}
		request.setDevice (device);
		
		extend (request);
		
	}
	
	protected String [] endpoint (ApiRequest request, String [] endpoint) {
		return endpoint;
	}

	protected String [] path (ApiRequest request, String [] path) {
		return path;
	}

	protected void extend (AbstractApiRequest request) {
		// extend by implementor
	}
	
	protected void set (Target target, AbstractApiRequest request, String [] aEndpoint, String [] aPath, int defaultIndex) {
		JsonObject oTarget 	= Json.getObject (spec, target.name ());
		
		Object value = Json.getString (oTarget, Spec.Value);
		
		if (value == null) {
			Placeholder ph 		= Placeholder.valueOf (Json.getString (oTarget, Spec.Placeholder, Placeholder.path.name ()));
			int 		index 	= Json.getInteger (oTarget, Spec.Index, defaultIndex);
			
			String [] array = null;
			
			switch (ph) {
				case endpoint:
					array = aEndpoint;
					break;
		
				case path:
					array = aPath;
					break;
		
				default:
					break;
			}
			
			if (array == null || (index >= array.length && !target.equals (Target.resource))) {
				throw new RuntimeException ("can't resolve " + target.name () + " from " + ph.name () + " using index " + index);
			}
			
			if (array == null || array.length == 0) {
				value = null;
			} else {
				value = target.equals (Target.resource) ? Lang.moveLeft (array, index) : array [index];
			}
		}
		
		switch (target) {
			case space:
				String space = (String)value;
				request.setSpace (Json.getString (Json.getObject (oTarget, Spec.Mapping), space, space));
				break;
	
			case api:
				String api = (String)value;
				request.setApi (Json.getString (Json.getObject (oTarget, Spec.Mapping), api, api));
				break;
	
			case resource:
				request.setResource ((String [])value);
				break;
	
			default:
				break;
		}
	}

}

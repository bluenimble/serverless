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
package com.bluenimble.platform.server.impls;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.impls.AbstractApiRequest;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.ApiRequestVisitor;

public class DefaultApiRequestVisitor implements ApiRequestVisitor {

	private static final long serialVersionUID = 1782406079539122227L;
	
	enum Target {
		space,
		api,
		resource
	}

	enum Placeholder {
		endpoint,
		path
	}

	interface Spec {
		String Static 		= "static";
		String Value 		= "value";
		String Placeholder 	= "placeholder";
		String Index 		= "index";
		String Mapping 		= "mapping";
	}

	private JsonObject spec;
	
	@Override
	public void visit (AbstractApiRequest request) {
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
		
		// resolve space, api and resource
		set (Target.space, 		request, aEndpoint, aPath, 0);
		set (Target.api, 		request, aEndpoint, aPath, 1);
		set (Target.resource, 	request, aEndpoint, aPath, 2);
		
		// set device
		JsonObject device = request.getDevice ();
		if (device == null) {
			device = new JsonObject ();
		}
		request.setDevice (device);
	}
	
	protected void enrich (AbstractApiRequest request) {
		// override by implementor
	}
	
	private void set (Target target, AbstractApiRequest request, String [] aEndpoint, String [] aPath, int defaultIndex) {
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

	public JsonObject getSpec () {
		return spec;
	}
	public void setSpec (JsonObject spec) {
		this.spec = spec;
	}

	@Override
	public String [] guess (String host, String space, String api, String service) {
		String endpoint = host;
		String path 	= service;
		
		JsonObject apiTarget 	= Json.getObject (spec, Target.api.name ());
		Placeholder apiPH 		= Placeholder.valueOf (Json.getString (apiTarget, Spec.Placeholder, Placeholder.path.name ()));
		
		if (apiPH.equals (Placeholder.path)) {
			path = Lang.SLASH + api + path;
		} 
		
		JsonObject spaceTarget 	= Json.getObject (spec, Target.space.name ());
		Placeholder spacePH 	= Placeholder.valueOf (Json.getString (spaceTarget, Spec.Placeholder, Placeholder.path.name ()));
		
		if (spacePH.equals (Placeholder.path)) {
			path = Lang.SLASH + space + path;
		} else {
			endpoint = space + Lang.DOT + endpoint;
		}
		
		return new String [] { endpoint, path };
	}

}

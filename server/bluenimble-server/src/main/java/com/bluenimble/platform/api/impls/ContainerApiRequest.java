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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.iterators.EmptyIterator;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable (name = "ApiRequest")
public class ContainerApiRequest extends AbstractApiRequest {

	private static final long serialVersionUID = -3828809548431662110L;

	private static final EmptyIterator<String> 	EmptyIterator	= new EmptyIterator<String> ();

	private static final String Origin 	= "Container";
	private static final String Scheme 	= "baas";
	
	private String 							path;
	private String 							endpoint;
	
	private Map<String, Object> 			headers;
	private Map<String, ApiStreamSource> 	streams;
		
	public ContainerApiRequest () {
	}

	public ContainerApiRequest (ApiRequest parentRequest, String agent, ApiSpace.Endpoint endpoint) {
		super (parentRequest);
		this.space 			= endpoint.space ();
		this.channel 		= ApiRequest.Channels.container.name ();
		this.api 			= endpoint.api ();
		this.verb 			= endpoint.verb ();
		this.endpoint 		= parentRequest.getEndpoint ();
		
		this.resource		= endpoint.resource ();
		if (resource == null || resource.length == 0) {
			this.resource 	= new String [] { Lang.SLASH };
			this.path 		= Lang.SLASH;
		} else {
			this.path 		= Lang.SLASH + Lang.join (resource, Lang.SLASH);
		}
		
		this.device 		= (JsonObject)new JsonObject ()
									.set (ApiRequest.Fields.Device.Agent, agent)
									.set (ApiRequest.Fields.Device.Origin, Origin)
									.set (ApiRequest.Fields.Device.Language, parent.getLang ());

		this.node 			= parentRequest.getNode ();
	}

	@Override
	public String getEndpoint () {
		return endpoint;
	}

	@Override
	public String getLang () {
		return parent.getLang ();
	}

	@Override
	public String getScheme () {
		return Scheme;
	}

	@Override
	public void set (String name, Object value, Scope... scopes) {
		
		Scope scope = Scope.Parameter;
		
		if (scopes != null && scopes.length > 0) {
			scope = scopes [0];
		}
		
		switch (scope) {
			case Stream:
				if (streams == null) {
					streams = new HashMap<String, ApiStreamSource> ();
				}
				streams.put (name, (ApiStreamSource)value);
				break;
			
			default:
				super.set (name, value, scopes);
		}
	}

	@Override
	public Iterator<String> keys (Scope scope) {
		switch (scope) {
			case Header:
				if (headers == null || headers.isEmpty ()) {
					return EmptyIterator;
				}
				return headers.keySet ().iterator ();
			case Stream:
				if (streams == null || streams.isEmpty ()) {
					return EmptyIterator;
				}
				return streams.keySet ().iterator ();
			case Parameter:
				if (application == null) {
					return EmptyIterator;
				} 
				return application.keySet ().iterator ();
			default:
				break;
		}
		return EmptyIterator;
	}
	
	@Override
	protected void setHeader (String name, Object value) {
		if (headers == null) {
			headers = new HashMap<String, Object> ();
		}
		headers.put (name, value);
	}

	@Override
	protected Object getByScope (String name, Scope scope) {
		switch (scope) {
			case Header:
				if (headers == null) {
					return null;
				}
				return headers.get (name);
			case Stream:
				if (streams == null) {
					return null;
				}
				return streams.get (name);
			case Parameter:
				if (application == null) {
					return null;
				}
				return application.get (name);
			default:
				break;
		}
		return null;
	}
	
	@Override
	public void destroy () {
		
		super.destroy ();
		
		if (headers != null) {
			headers.clear ();
			headers = null;
		}
		if (streams != null) {
			streams.clear ();
			streams = null;
		}
	}

	@Override
	public String getPath () {
		return path;
	}

}

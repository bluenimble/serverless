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
import java.util.Locale;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.iterators.EmptyIterator;
import com.bluenimble.platform.json.JsonObject;

public class SimpleApiRequest extends AbstractApiRequest {
	
	private static final long serialVersionUID = -3037741556600499160L;

	private static final EmptyIterator<String> 	EmptyIterator	= new EmptyIterator<String> ();

	protected 	Map<String, Object> headers;
	
	private 	String 				scheme;
	private 	String 				endpoint;
	private 	String 				path;
	
	@SuppressWarnings("unchecked")
	public SimpleApiRequest (JsonObject payload) {
		this.channel 	= Json.getString (payload, ApiRequest.Fields.Channel);
		this.verb 		= ApiVerb.valueOf (Json.getString (payload, ApiRequest.Fields.Verb, ApiVerb.GET.name ()).toUpperCase ());
		this.scheme 	= Json.getString (payload, ApiRequest.Fields.Scheme);
		this.endpoint 	= Json.getString (payload, ApiRequest.Fields.Endpoint);
		this.path		= Json.getString (payload, ApiRequest.Fields.Path);
		this.device		= Json.getObject (payload, ApiRequest.Fields.Device.class.getSimpleName ().toLowerCase ());
		this.node 			= new JsonObject ();
				
		JsonObject oParameters = Json.getObject (payload, ApiRequest.Fields.Data.Parameters);
		if (!Json.isNullOrEmpty (oParameters)) {
			application = new HashMap<String, Object> ();
			application.putAll (oParameters);
		}
		
		JsonObject oHeaders = Json.getObject (payload, ApiRequest.Fields.Data.Headers);
		if (!Json.isNullOrEmpty (oHeaders)) {
			headers = new HashMap<String, Object> ();
			headers.putAll (oHeaders);
		}
		
	}

	public SimpleApiRequest (String channel, ApiVerb verb, String scheme, String endpoint, String path, String origin, String agent) {
		this.channel 		= channel;
		this.verb 			= verb;
		this.scheme 		= scheme;
		this.endpoint		= endpoint;
		this.path			= path;
		this.device 		= (JsonObject)new JsonObject ()
									.set (ApiRequest.Fields.Device.Agent, agent)
									.set (ApiRequest.Fields.Device.Origin, origin)
									.set (ApiRequest.Fields.Device.Language, Locale.ENGLISH.getLanguage ());
		node = new JsonObject ();
	}

	@Override
	public String getEndpoint () {
		return endpoint;
	}

	@Override
	public String getScheme () {
		return scheme;
	}

	@Override
	public String getPath () {
		return path;
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
				return EmptyIterator;
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
				return null;
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
	}
}

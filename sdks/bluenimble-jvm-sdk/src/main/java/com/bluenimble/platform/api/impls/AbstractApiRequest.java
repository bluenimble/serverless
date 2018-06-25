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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequestTrack;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.json.JsonObject;

public abstract class AbstractApiRequest extends AbstractApiContext implements ApiRequest {

	private static final long serialVersionUID = -3224649113967307826L;

	private static final String Version = "version"; 
	
	protected 	String 					id;
	protected 	Date 					timestamp;
	
	protected 	JsonObject 				node;
	protected 	String 					space;
	protected 	String 					api;
	protected 	String					channel;
	
	protected 	JsonObject 				device;
	
	protected 	String []				resource;
	
	protected 	Map<String, Object> 	application;
	
	protected 	ApiService				service;
	protected	ApiRequest 				parent;
	
	protected 	JsonObject 				json;
	private 	int						version;
	
	protected 	ApiVerb 				verb;
	
	protected 	ApiRequestTrack 		track;
	
	protected AbstractApiRequest (ApiRequest parent) {
		this.parent = parent;
		id 			= Lang.UUID (10);
		timestamp	= new Date ();
	}

	protected AbstractApiRequest () {
		this (null);
	}

	protected abstract void 	setHeader 	(String name, Object value);
	protected abstract Object 	getByScope 	(String name, Scope scope);

	@Override
	public JsonObject getNode () {
		return node;
	}
	
	@Override
	public ApiService getService () {
		return service;
	}
	
	@Override
	public ApiRequest getParent () {
		return parent;
	}
	
	@Override
	public String getId () {
		return id;
	}
	
	@Override
	public Date getTimestamp () {
		return timestamp;
	}
	
	@Override
	public String getSpace () {
		return space;
	}
	
	@Override
	public String getApi () {
		return api;
	}
	
	@Override
	public String getChannel () {
		return channel;
	}

	@Override
	public String [] getResource () {
		return resource;
	}
	
	@Override
	public JsonObject getDevice () {
		return device;
	}
	
	@Override
	public void set (String name, Object value, Scope... scopes) {
		
		Scope scope = Scope.Parameter;
		
		if (scopes != null && scopes.length > 0) {
			scope = scopes [0];
		}
		
		switch (scope) {
			case Parameter:
				if (value == null) {
					if (application != null) {
						application.remove (name);
					}
					return;
				}
				
				if (application == null) {
					application = new HashMap<String, Object> ();
				}
				
				++version;
				
				application.put (name, value);
				break;
			
			case Header:
				setHeader (name, value);
				break;
			
			default:
				break;
		}
	}

	@Override
	public ApiVerb getVerb () {
		return verb;
	}
	
	@Override
	public String getLang () {
		return Json.getString (device, Fields.Device.Language, Locale.ENGLISH.getLanguage ());
	}

	@Override
	public Object get (String name, Scope... scopes) {
		if (scopes == null || scopes.length == 0) {
			scopes = new Scope [] {Scope.Parameter};
		}
		
		for (Scope s : scopes) {
			Object v = getByScope (name, s);
			if (v != null) {
				return v;
			}
		}
		return null;
	}

	@Override
	public String toString () {
		return toJson ().toString (2);
	}
	
	public void setResource (String [] resource) {
		this.resource = resource;
	}

	public void setApi (String api) {
		this.api = api;
	}

	public void setSpace (String space) {
		this.space = space;
	}

	public void setDevice (JsonObject device) {
		this.device = device;
	}

	@Override
	public JsonObject toJson () {
		int version = Json.getInteger (json, Version, 0);
		if (json == null || version < this.version) {
			json = _toJson ();
		}
		return json;
	}
	
	@Override
	public ApiRequestTrack track () {
		return track;
	}
	
	@Override
	public void track (ApiRequestTrack track) {
		this.track = track;
	}
	
	@Override
	public Recyclable getRecyclable (String name) {
		
		Recyclable r = null; 
				
		if (parent != null) {
			r = parent.getRecyclable (name);
		}
		
		if (r != null) {
			return r;
		}
		
		if (recyclable == null) {
			return null;
		}
		
		return recyclable.get (name);
	}
	
	@Override
	public void destroy () {
		parent = null;
		id = null;
		resource = null;
		service = null;
		api = null;
		if (application != null) {
			Object transport 	= application.get (Transport);
			Object output 		= application.get (Output);
			
			application.clear ();
			
			application.put (Transport, transport);
			application.put (Output, output);
			
			if (application.isEmpty ()) {
				application = null;
			}
		}
		recycle ();
	}

	public void setService (ApiService service) {
		this.service = service;
	}

	protected JsonObject _toJson () {

		JsonObject device = getDevice ();
		if (device == null) {
			device = new JsonObject ();
		}
		JsonObject data = new JsonObject ();
		
		JsonObject json = new JsonObject ();
		
		String sResource = Lang.join (getResource (), Lang.SLASH);
		
		json.set (Fields.Id, getId ());
		json.set (Fields.Version, version);
		json.set (Fields.Verb, getVerb ().name ());
		json.set (Fields.Node.class.getSimpleName ().toLowerCase (), node);
		json.set (Fields.Channel, getChannel ());
		json.set (Fields.Space, getSpace ());
		json.set (Fields.Api, getApi ());
		json.set (Fields.Scheme, getScheme ());
		json.set (Fields.Endpoint, getEndpoint ());
		json.set (Fields.Path, getPath ());
		json.set (Fields.Resource, sResource);
		json.set (Fields.Timestamp, getTimestamp ());
		json.set (Fields.Device.class.getSimpleName ().toLowerCase (), device);
		json.set (Fields.Data.class.getSimpleName ().toLowerCase (), data);
		
		Map<String, Object> geo = geo ();
		if (geo != null) {
			device.set (Fields.Device.GeoLocation, geo);
		}
		
		Map<String, Object> parameters = jParameters ();
		if (parameters != null && !parameters.isEmpty ()) {
			data.set (Fields.Data.Parameters, parameters);
		}
		Map<String, Object> streams = jStreams ();
		if (streams != null && !streams.isEmpty ()) {
			data.set (Fields.Data.Streams, streams);
		}
		Map<String, Object> headers = jHeaders ();
		if (headers != null && !headers.isEmpty ()) {
			data.set (Fields.Data.Headers, headers);
		}
		
		return json;
	}

	private Map<String, Object> jParameters () {
		Iterator<String> keys = keys (ApiRequest.Scope.Parameter);
		if (keys == null) {
			return null;
		}
		
		Map<String, Object> parameters = new HashMap<String, Object> ();
		
		while (keys.hasNext ()) {
			String key = keys.next ();
			parameters.put (key, get (key, ApiRequest.Scope.Parameter));
		}
		
		return parameters;
	}

	private Map<String, Object> jHeaders () {
		Iterator<String> keys = keys (ApiRequest.Scope.Header);
		if (keys == null) {
			return null;
		}
		
		Map<String, Object> headers = new HashMap<String, Object> ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			if (key.equals (ApiHeaders.GeoLocation)) {
				continue;
			}
			headers.put (key, get (key, ApiRequest.Scope.Header));
		}
		return headers;
	}
	
	private Map<String, Object> jStreams () {
		Iterator<String> keys = keys (ApiRequest.Scope.Stream);
		if (keys == null) {
			return null;
		}
		
		Map<String, Object> streams = new HashMap<String, Object> ();
		
		while (keys.hasNext ()) {
			String key = keys.next ();
			streams.put (key, get (key, ApiRequest.Scope.Stream));
		}
		
		return streams;
	}

	private Map<String, Object> geo () {
		String geo = (String)get (ApiHeaders.GeoLocation, ApiRequest.Scope.Header);
		if (Lang.isNullOrEmpty (geo)) {
			return null;
		}
		String [] aGeo = Lang.split (geo, Lang.SEMICOLON);
		if (aGeo == null || aGeo.length < 2) {
			return null;
		}
		Map<String, Object> mGeo = new HashMap<String, Object> ();
		for (String attr : aGeo) {
			String [] keyValue = Lang.split (attr, Lang.EQUALS);
			if (keyValue == null || keyValue.length < 2) {
				continue;
			}
			Double dv = null;
			try {
				dv = Double.valueOf (keyValue [1]);
			} catch (NumberFormatException nex) {
				break;
			}
			mGeo.put (keyValue [0].toLowerCase (), dv);
		}
		return mGeo;
	}

}

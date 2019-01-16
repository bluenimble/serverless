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
package com.bluenimble.platform.api;

import java.util.Date;
import java.util.Iterator;

import com.bluenimble.platform.json.JsonObject;

public interface ApiRequest extends ApiContext {
	
	interface Interceptors {
		String Bypass				= "Container.Request.Intercept.Bypass";
		String Response 			= "Container.Request.Intercept.Response";
	}

	String Consumer				= "Container.Request.Consumer";
	String Caller 				= "Container.Request.Caller";
	
	String Output 				= "Container.Request.Output";

	String SelectedMedia 		= "Container.Request.SelectedMedia";
	String MediaSelector 		= "Container.Request.MediaSelector";
	
	String ResponseStatus 		= "Container.Request.ResponseStatus";

	String Transport			= "transport";
	String Payload				= "payload";
	
	interface ForEachCallback {
		void visit (String key, Object value);
	}

	interface Fields {
		String Id 				= "id";
		String Version			= "version";
		String Channel			= "channel";
		String Space 			= "space";
		String Api 				= "api";
		String Verb 			= "verb";
		String Scheme 			= "scheme";
		String Endpoint 		= "endpoint";
		String Resource			= "resource";
		String Path				= "path";
		
		String RequestReference	= "reference";
		
		String Timestamp 		= "timestamp";
		
		interface Node 		{
			String Id 			= "id";
			String Type 		= "type";
			String Version 		= "version";
			String StartTime 	= "sts";
		}
		
		interface Device 		{
			String Origin 		= "origin";
			String Language 	= "lang";
			String Agent 		= "agent";
			String Os 			= "os";
			String Category 	= "category";
			String Country 		= "country";
			String GeoLocation 	= "geo";
		}
		
		interface Data 		{
			String Parameters 	= "params";
			String Headers 		= "headers";
			String Streams 		= "streams";
		}
		
	}
	
	enum Scope {
		Parameter,
		Header,
		Stream
	}
	
	enum Channels {
		container
	}
	
	String 				getEndpoint 	();
	String 				getScheme 		();

	JsonObject 			getNode 		();
	
	String 				getSpace 		();
	String 				getApi 			();
	
	String 				getId 			();
	
	Date 				getTimestamp 	();
	
	String 				getChannel 		();
	
	ApiVerb 			getVerb 		();

	String []			getResource 	();

	String 				getPath 		();

	String 				getLang 		();
	JsonObject 			getDevice 		();
	
	Object 				get 			(String name, Scope... 	scope);
	void 				set 			(String name, Object 	value, Scope... 	scope);
	Iterator<String>	keys 			(Scope 	scope);
	void				forEach			(Scope 	scope, ForEachCallback callback);
	
	JsonObject			toJson 			();
	
	ApiRequestTrack 	track 			();
	void 				track 			(ApiRequestTrack track);
	
	ApiService			getService 		();
	
	ApiRequest			getParent 		();
	
	void 				destroy 		();
	
}


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
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceSpi;
import com.bluenimble.platform.api.ApiStatus;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.impls.spis.NoLogicApiServiceSpi;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.BeanUtils;
import com.bluenimble.platform.reflect.ClassLoaderRegistry;
import com.bluenimble.platform.server.utils.ConfigKeys;

public class ApiServiceImpl implements ApiService {

	private static final long serialVersionUID = 920163706776702151L;
	
	private static final ApiServiceSpi DefaultSpi = new NoLogicApiServiceSpi ();
	
	interface SpecExt {
		String Index = "index";
		String Unary = "unary";
	}
	
	private 	ApiResource 			resource;
	
	protected 	JsonObject 				source;
	
	protected 	ApiVerb 				verb;
	protected 	String					endpoint;

	protected 	ApiStatus				status;
	protected 	JsonObject 				failure;
	
	protected 	ApiServiceSpi			spi 			= DefaultSpi;
	
	private   	JsonObject				accessors;
	
	private 	Object					helper;
	
	private 	ApiImpl					api;
	
	public ApiServiceImpl (ApiResource resource, JsonObject source, ApiImpl api) {
		
		this.resource 	= resource;
		
		this.source 	= source;
		
		// set id if not found
		if (Lang.isNullOrEmpty (Json.getString (source, ApiService.Spec.Id))) {
			String path = this.resource.path ();
			if (path.startsWith (ConfigKeys.Folders.Services + Lang.SLASH)) {
				path = path.substring ((ConfigKeys.Folders.Services + Lang.SLASH).length ());
			}
			if (path.endsWith (ConfigKeys.JsonExt)) {
				path = path.substring (0, path.length () - ConfigKeys.JsonExt.length ());
			}
			String [] parts = Lang.split (path, Lang.SLASH);
			for (int i = 0; i < parts.length; i++) {
				parts [i] = Lang.capitalizeFirst (parts [i]);
			}
			source.set (ApiService.Spec.Id, Lang.join (parts, Lang.DOT));
		}

		this.api 		= api;
		
		setStatus (Json.getString (this.source, Spec.Status));
		
		verb = ApiVerb.valueOf (
			Json.getString (source, Spec.Verb, ApiVerb.GET.name ()).toUpperCase ()
		);
		
		resolveEndpoint  ();	
		
		if (status == null) {
			setStatus (ApiStatus.Stopped, false);
		}
		
	}
	
	@Override
	public ApiVerb getVerb () {
		return verb;
	}

	@Override
	public String getEndpoint () {
		return endpoint;
	}

	@Override
	public String getName () {
		String name = Json.getString (source, Spec.Name);
		if (Lang.isNullOrEmpty (name)) {
			return resource.name ();
		}
		return name;
	}

	@Override
	public String getDescription () {
		return Json.getString (source, Spec.Description);
	}

	@Override
	public JsonObject getRuntime () {
		return Json.getObject (source, Spec.Runtime);
	}

	@Override
	public JsonObject getFeatures () {
		return Json.getObject (source, Spec.Features);
	}

	@Override
	public JsonObject getMedia () {
		JsonObject media = Json.getObject (source, Spec.Media.class.getSimpleName ().toLowerCase ());
		if (media == null) {
			media = api.getMedia ();
		} else if (media.isEmpty ()) {
			return null;
		}
		return media;
	}

	@Override
	public JsonObject getCustom () {
		return Json.getObject (source, Spec.Custom);
	}

	@Override
	public JsonObject getSecurity () {
		return Json.getObject (source, Spec.Security.class.getSimpleName ().toLowerCase ());
	}

	@Override
	public ApiStatus status () {
		return status;
	}

	@Override
	public void pause () {
		if (status.equals (ApiStatus.Failed)) {
			return;
		}
		setStatus (ApiStatus.Paused, true);
	}

	@Override
	public void resume () {
		if (status.equals (ApiStatus.Failed)) {
			return;
		}
		setStatus (ApiStatus.Running, true);
	}
	
	@Override
	public JsonObject getFailure () {
		return failure;
	}

	@Override
	public ApiServiceSpi getSpi () {
		return spi;
	}

	@Override
	public JsonObject toJson () {
		return source;
	}

	@Override
	public Object getHelper () {
		return helper;
	}

	@Override
	public void setHelper (Object helper) {
		this.helper = helper;
	}

	public JsonObject accessors () {
		return accessors;
	}

	protected void dettachSpi () {
		spi = DefaultSpi; 
	}
	
	public void setStatus (ApiStatus status, boolean save) {
		this.status = status;
		source.set (Api.Spec.Status, status.name ());
		if (save) {
			updateSource ();
		}
	}
	
	private void updateSource () {
		if (source == null || source.isEmpty ()) {
			throw new RuntimeException ("empty service definition");
		}
		
		if (status != null) {
			source.set (Api.Spec.Status, status.name ());
		}
		
		api.space.statusManager.update (api, this, status);
	}

	public void failed (String message) {
		failure = (JsonObject)new JsonObject ().set (ApiResponse.Error.Message, message);
		setStatus (ApiStatus.Failed, false);
	}
	public void failed (Exception ex) {
		failure = Lang.toError (ex);
		setStatus (ApiStatus.Failed, false);
	}
	
	protected void attachSpi (ClassLoader classloader, ClassLoaderRegistry clRegistry) {
		JsonObject oSpi = Json.getObject (source, ConfigKeys.Spi);
		
		if (oSpi != null) {
			try {
				spi = (ApiServiceSpi)BeanUtils.create (classloader, oSpi, clRegistry);
			} catch (Exception ex) {
				failed (ex);
			}
		} 
	}
	
	private void resolveEndpoint () {
		
		String endpoint = Json.getString (source, ApiService.Spec.Endpoint);
		if (Lang.isNullOrEmpty (endpoint)) {
			return;
		}
		
		if (endpoint.indexOf (Lang.COLON) < 0) {
			this.endpoint = endpoint;
			return;
		}
		
		if (endpoint.startsWith (Lang.SLASH)) {
			endpoint = endpoint.substring (1);
		}
		
		boolean doubleColonFound = false;
		
		JsonObject oAccessors = new JsonObject ();
		
		String [] accessors = Lang.split (endpoint, Lang.SLASH);
		for (int i = 0; i < accessors.length; i++) {
			String accessor = accessors [i];
			if (accessor.startsWith (Lang.COLON + Lang.COLON)) {
				if (doubleColonFound) {
					status = ApiStatus.Failed;
					failure = (JsonObject) new JsonObject ().set (ApiResponse.Error.Message, "an endpoint can't contain more than 1 occurence of '" + Lang.COLON + Lang.COLON + "'");
					break;
				}
				doubleColonFound = true;
				endpoint = Lang.replace (endpoint, accessor, Lang.STAR + Lang.STAR);
				oAccessors.set (accessor.substring (2), new JsonObject ().set (SpecExt.Index, i).set (SpecExt.Unary, false));
			} else if (accessor.startsWith (Lang.COLON)) {
				endpoint = Lang.replace (endpoint, accessor, Lang.STAR);
				oAccessors.set (accessor.substring (1), new JsonObject ().set (SpecExt.Index, i).set (SpecExt.Unary, true));
			}
		}
		
		this.accessors 	= oAccessors;
		
		this.endpoint 	= Lang.SLASH + endpoint;
		
	}
	
	private void setStatus (String status) {
		if (status == null) {
			return;
		}
		try {
			this.status = ApiStatus.valueOf (status);
		} catch (Exception ex) {
			// IGNORE
		}
	}

}

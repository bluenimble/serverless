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

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManager;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServicesManager;
import com.bluenimble.platform.api.ApiServicesManagerException;
import com.bluenimble.platform.api.ApiStatus;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.FieldType;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.ClassLoaderRegistry;
import com.bluenimble.platform.server.impls.fs.ApiServiceSet;
import com.bluenimble.platform.server.utils.ConfigKeys;

public class DefaultApiServicesManager implements ApiServicesManager {

	private static final long serialVersionUID = 119185947209776190L;
	
	protected Map<ApiVerb, ApiServiceSet> 	services 		= new LinkedHashMap<ApiVerb, ApiServiceSet> ();
	protected Map<String, ApiService> 		servicesById 	= new LinkedHashMap<String, ApiService> ();
	
	private ApiImpl 						api;
	private ClassLoaderRegistry 			clRegistry;
	
	public DefaultApiServicesManager (ClassLoaderRegistry clRegistry) {
		this.clRegistry = clRegistry;
	}

	@Override
	public void load (Api api) throws ApiServicesManagerException {
		this.api = (ApiImpl)api;
		ApiResourcesManager rmgr = api.getResourcesManager ();
		try {
			load (rmgr, rmgr.get (new String [] { ConfigKeys.Folders.Services }));
		} catch (ApiResourcesManagerException e) {
			throw new ApiServicesManagerException (e.getMessage (), e);
		}
	}

	@Override
	public void onStart (final ApiContext context) throws ApiServicesManagerException {
		// call services spi onStart
		list (new Selector () {
			@Override
			public boolean select (ApiService service) {
				startService (service, context, ApiStatus.Paused.equals (service.status ()));
				return false;
			}
		});
	}

	@Override
	public void delete (ApiVerb verb, String endpoint)
			throws ApiServicesManagerException {
		
		ApiServiceSet set = services.get (verb);
		if (set == null) {
			throw new ApiServicesManagerException ("service [" + verb + " " + endpoint + "] not found");
		}
		
		ApiService service = set.get (endpoint);
		if (service == null) {
			throw new ApiServicesManagerException ("service [" + verb + " " + endpoint + "] not found");
		}
		
		if (!ApiStatus.Failed.equals (service.status ()) && !ApiStatus.Stopped.equals (service.status ())) {
			throw new ApiServicesManagerException ("can't delete service [" + verb + " " + endpoint + "]. Status=" + service.status ());
		}
		
		set.remove (endpoint);
		
	}

	@Override
	public ApiService get (ApiVerb verb, String endpoint) {
		
		ApiServiceSet set = services.get (verb);
		if (set == null) {
			return null;
		}
		
		ApiService service = set.get (resolveEndpoint (endpoint));
		if (service == null) {
			return null;
		}
		
		return service;
		
	}

	@Override
	public ApiService getById (String id) {
		return servicesById.get (id);
	}

	@Override
	public ApiService put (ApiResource resource) throws ApiServicesManagerException {
		
		Exception failure = null;
		
		JsonObject source = null;
		InputStream stream = null;
		try {
			stream = resource.toInput ();
			source = Json.load (stream);
		} catch (Exception ex) {
			failure = ex;;
		} finally {
			IOUtils.closeQuietly (stream);
		}
		
		if (failure != null) {
			source = (JsonObject)new JsonObject ().set (ApiService.Spec.Status, ApiStatus.Failed.name ()).set (ApiService.Spec.Endpoint, resource.path ());
		}
		
		ApiServiceImpl service = new ApiServiceImpl (resource, source, api);
		
		if (failure != null) {
			service.failed (failure);
		}
		
		if (failure == null) {
			if (exists (service.getVerb (), service.getEndpoint ())) {
				delete (service.getVerb (), service.getEndpoint ());
			}
		}
		
		ApiServiceSet set = services.get (service.getVerb ());
		if (set == null) {
			set = new ApiServiceSet ();
			services.put (service.getVerb (), set);
		}
		
		set.add (service);
		
		if (service.getId () != null) {
			servicesById.put (service.getId (), service);
		}
		
		return service;
		
	}

	@Override
	public void list (Selector selector) {
		if (selector == null || services.isEmpty ()) {
			return;
		}
		Iterator<ApiVerb> verbs = services.keySet ().iterator ();
		while (verbs.hasNext ()) {
			ApiVerb verb = verbs.next ();
			ApiServiceSet set = services.get (verb);
			Iterator<String> endpoints = set.endpoints ();
			if (endpoints == null) {
				continue;
			}
			while (endpoints.hasNext ()) {
				boolean cancel = selector.select (set.get (endpoints.next ()));
				if (cancel) {
					return;
				}
			}
		}
	}

	@Override
	public boolean isEmpty (ApiVerb verb) {
		if (services.isEmpty ()) {
			return true;
		}
		if (verb == null) {
			return isEmpty (ApiVerb.GET) && isEmpty (ApiVerb.POST) && isEmpty (ApiVerb.PUT) && isEmpty (ApiVerb.DELETE) && isEmpty (ApiVerb.HEAD);
		}
		ApiServiceSet set = services.get (verb);
		if (set == null) {
			return true;
		}
		return set.isEmpty ();
	}

	@Override
	public boolean exists (ApiVerb verb, String endpoint) {
		return get (verb, endpoint) != null;
	}

	@Override
	public void onStop (final ApiContext context) {
		
		final Tracer tracer = api.tracer ();
		
		// stop all services
		list (new Selector () {
			@Override
			public boolean select (ApiService service) {
				try {
					stopService (service, context, false);
				} catch (Exception ex) {
					tracer.log (Tracer.Level.Error, "ApiSpi.onStop casued an error", ex);
				}
				return false;
			}
		});
		
	}
	
	private void load (final ApiResourcesManager rmgr, final ApiResource resource) {
		resource.children (new ApiResource.Selector () {
			@Override
			public boolean select (String name, boolean isFolder) {
				ApiResource child = null;
				try {
					child = rmgr.get (Lang.add (Lang.split (resource.path (), Lang.SLASH), new String [] { name }));
				} catch (Exception ex) {
					throw new RuntimeException (ex.getMessage (), ex);
				}
				if (isFolder) {
					load (rmgr, child);
				} else {
					if (!child.name ().endsWith (ConfigKeys.JsonExt)) {
						return false;
					}
					try {
						put (child);
					} catch (Exception ex) {
						throw new RuntimeException (ex.getMessage (), ex);
					}
				}
				return false;
			}
		});
	}

	@Override
	public void start (ApiVerb verb, String endpoint) throws ApiServicesManagerException {
		ApiService service = get (verb, endpoint);
		if (service == null) {
			throw new ApiServicesManagerException ("service [" + verb + " " + endpoint + "] not found");
		}
		if (!service.status ().equals (ApiStatus.Stopped)) {
			throw new ApiServicesManagerException ("unnable to start service [" + verb + " " + endpoint + "]. Status=" + service.status ());
		}

		startService (service, null, false);
	}
	
	private void startService (ApiService service, ApiContext context, boolean pause) {
		ApiServiceImpl sImpl = (ApiServiceImpl)service;
		
		// call spi onStart
		boolean newContext = context == null;
		try {
			// attach api
			sImpl.attachSpi (api.getClassLoader (), clRegistry);
			
			if (newContext) {
				context = new DefaultApiContext ();
			}
			service.getSpi ().onStart (api, service, context);
		} catch (ApiManagementException ex) {
			sImpl.failed (ex);
		} finally {
			if (newContext) {
				context.recycle ();
			}
		}
		
		// update/save status
		if (!ApiStatus.Failed.equals (service.status ())) {
			sImpl.setStatus (pause ? ApiStatus.Paused : ApiStatus.Running, true);
		}		
	}

	@Override
	public void stop (ApiVerb verb, String endpoint) throws ApiServicesManagerException {
		ApiService service = get (verb, endpoint);
		if (service == null) {
			throw new ApiServicesManagerException ("service [" + verb + " " + endpoint + "] not found");
		}
		if (!service.status ().equals (ApiStatus.Running) || !service.status ().equals (ApiStatus.Paused)) {
			throw new ApiServicesManagerException ("unnable to stop service [" + verb + " " + endpoint + "]. Status=" + service.status ());
		}

		stopService (service, null, true);
	}
	
	// group by
	public JsonObject groupBy (String property, String groupItemKey, GroupingFlow flow) {
		JsonObject groups = new JsonObject ();
		if (Lang.isNullOrEmpty (property)) {
			return groups;
		}
		
		if (flow == null) {
			flow = NoGroupingFlow;
		}
		
		String [] path = Lang.split (property, Lang.DOT);
		
		for (ApiServiceSet set : services.values ()) {
			Iterator<String> endpoints = set.endpoints ();
 			while (endpoints.hasNext ()) {
 				String endpoint = endpoints.next ();
 				ApiServiceImpl s = (ApiServiceImpl)set.get (endpoint);
 				Object pv = Json.find (s.source, path);
 				if (pv == null) {
 					continue;
 				}
 				String groupKey = String.valueOf (pv);
 				
 				// flow onGroupKey
 				groupKey = flow.onGroupKey (api, groupKey);
 				
 				if (Lang.isNullOrEmpty (groupKey)) {
 					continue;
 				}
 				
 				JsonObject group = Json.getObject (groups, groupKey);
 				if (group == null) {
 					group = new JsonObject ();
 					groups.set (groupKey, group);
 				}
 				
 				String type = Json.getString (s.source, ApiServiceValidator.Spec.Type, FieldType.String);
 				
 				if (!Lang.isNullOrEmpty (groupItemKey)) {
 					Object gikValue = Json.find (s.source, groupItemKey);
 					if (gikValue == null) {
 						if (ApiService.Spec.Verb.equals (groupItemKey)) {
 							gikValue = ApiVerb.GET.name ().toLowerCase ();
 						} else {
 	 						continue;
 						}
 					}
 					JsonObject oService = s.source.duplicate ();
 					oService = flow.onService (api, oService, FieldType.Object.equalsIgnoreCase (type) || api.getServiceValidator ().isCustomType (type));
 					oService.remove (property);
 					oService.remove (groupItemKey);
 					group.set (String.valueOf (gikValue), oService);
 				} else {
 					JsonArray items = Json.getArray (group, ApiOutput.Defaults.Items);
 					if (items == null) {
 						items = new JsonArray ();
 						group.set (ApiOutput.Defaults.Items, items);
 					}
 					JsonObject oService = s.toJson ().duplicate ();
 					oService = flow.onService (api, oService, FieldType.Object.equalsIgnoreCase (type) || api.getServiceValidator ().isCustomType (type));
 					oService.remove (property);
 					items.add (oService);
 				}
			}
		}
		
		return groups;
	}
	
	private void stopService (ApiService service, ApiContext context, boolean saveStatus) throws ApiServicesManagerException {
		ApiServiceImpl sImpl = (ApiServiceImpl)service;
		
		// call spi onStop
		boolean newContext = context == null;
		try {
			if (newContext) {
				context = new DefaultApiContext ();
			}
			service.getSpi ().onStop (api, service, context);
		} catch (ApiManagementException ex) {
			throw new ApiServicesManagerException (ex.getMessage (), ex);
		} finally {
			if (newContext) {
				context.recycle ();
			}
		}
		
		// attach api
		sImpl.dettachSpi ();
		
		// set service status
		sImpl.setStatus (ApiStatus.Stopped, saveStatus);
	}
	
	private String resolveEndpoint (String endpoint) {

		if (Lang.isNullOrEmpty (endpoint)) {
			return null;
		}
		
		if (endpoint.indexOf (Lang.COLON) < 0) {
			return endpoint;
		}
		
		boolean doubleColonFound = false;
		
		String [] accessors = Lang.split (endpoint, Lang.SLASH);
		for (int i = 0; i < accessors.length; i++) {
			String accessor = accessors [i];
			if (accessor.startsWith (Lang.COLON + Lang.COLON)) {
				if (doubleColonFound) {
					break;
				}
				doubleColonFound = true;
				endpoint = Lang.replace (endpoint, accessor, Lang.STAR + Lang.STAR);
			} else if (accessor.startsWith (Lang.COLON)) {
				endpoint = Lang.replace (endpoint, accessor, Lang.STAR);
			}
		}
		
		return endpoint;
		
	}

}

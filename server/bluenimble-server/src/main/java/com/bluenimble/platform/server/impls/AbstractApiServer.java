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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequestVisitor;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiResponse.Status;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiStatus;
import com.bluenimble.platform.api.CodeExecutor;
import com.bluenimble.platform.api.CodeExecutorException;
import com.bluenimble.platform.api.DescribeOption;
import com.bluenimble.platform.api.impls.AbstractApiRequest;
import com.bluenimble.platform.api.impls.ContainerApiRequest;
import com.bluenimble.platform.api.impls.ContainerApiResponse;
import com.bluenimble.platform.api.impls.SpaceThread;
import com.bluenimble.platform.api.media.ApiMediaProcessorRegistry;
import com.bluenimble.platform.api.security.ApiConsumerResolver;
import com.bluenimble.platform.api.security.ApiConsumerResolverAnnotation;
import com.bluenimble.platform.api.security.ApiRequestSigner;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.api.tracing.impls.NoTracing;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.cluster.ClusterPeer;
import com.bluenimble.platform.cluster.ClusterSerializer;
import com.bluenimble.platform.cluster.ClusterTask;
import com.bluenimble.platform.instance.InstanceDescriber;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginsRegistry;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.FeatureNotFoundException;
import com.bluenimble.platform.server.KeyStoreManager;
import com.bluenimble.platform.server.ServerFeature;
import com.bluenimble.platform.server.interceptor.ApiInterceptor;
import com.bluenimble.platform.server.maps.MapProvider;
import com.bluenimble.platform.server.tracking.BlankApiRequestTracker;
import com.bluenimble.platform.server.tracking.ServerRequestTracker;
import com.bluenimble.platform.server.utils.ApiUtils;
import com.bluenimble.platform.server.utils.ConfigKeys;
import com.bluenimble.platform.server.utils.DescribeUtils;

public abstract class AbstractApiServer implements ApiServer {
	
	private static final long serialVersionUID = -6264156530505699427L;
	
	interface Describe {
		
		String Hardware 	= "hardware";
		
		String Plugins		= "plugins";
		String Features 	= "features";
		String Providers 	= "providers";
		
		String Feature 		= "feature";
		
		String Vendor 		= "vendor";
		String Snapshot 	= "snapshot";
		
	}
	
	private static final 	String 			FeatureProtocol = "://";
	
	protected Date							startTime = new Date ();

	protected JsonObject 					descriptor;
	
	protected ClusterPeer 					peer;
	
	protected PluginsRegistry 				pluginsRegistry;
	
	protected KeyPair						keys;

	protected MapProvider 					mapProvider;
	
	protected ApiInterceptor 				interceptor;
	
	protected ApiServiceValidator 			serviceValidator;

	protected ApiRequestSigner 				requestSigner;
	
	protected ApiRequestVisitor				requestVisitor;
	
	protected KeyStoreManager				keyStoreManager;
	
	protected ApiMediaProcessorRegistry		mediaProcessorRegistry;
	
	private Map<String, ServerRequestTracker> 	
											requestTrackers		= new ConcurrentHashMap<String, ServerRequestTracker> ();

	private Map<String, ClusterSerializer> 		
											serializers 		= new ConcurrentHashMap<String, ClusterSerializer> ();
	
	private Map<String, ClusterTask> 		tasks 				= new ConcurrentHashMap<String, ClusterTask> ();

	private Map<String, ServerFeature> 		
											features 			= new ConcurrentHashMap<String, ServerFeature> ();
	
	private Map<Class<?>, String> 		
											customFeatures 		= new ConcurrentHashMap<Class<?>, String> ();
	
	private Map<String, ApiConsumerResolver> 	
											consumerResolvers 	= new ConcurrentHashMap<String, ApiConsumerResolver> ();

	private Map<String, ApiSpace> 			spaces 				= new ConcurrentHashMap<String, ApiSpace> ();
	
	protected JsonObject 					messages 			= new JsonObject ();
	
	protected InstanceDescriber				instanceDescriber;
	
	protected Tracer 						tracer 				= NoTracing.Instance;
	
	@Override
	public Date	startTime () {
		return startTime;
	}
	
	@Override
	public void	registerSerializer (ClusterSerializer serializer) {
		serializers.put (serializer.name (), serializer);
	}
	
	@Override
	public ClusterSerializer getSerializer (String name) {
		return serializers.get (name);
	}
	
	@Override
	public void	registerTask (ClusterTask task) {
		tasks.put (task.name (), task);
	}
	
	@Override
	public ClusterTask getTask (String name) {
		return tasks.get (name);
	}
	
	@Override
	public JsonObject getDescriptor () {
		return descriptor;
	}
	
	@Override
	public String getNamespace () {
		return id ();
	}
	
	@Override
	public JsonObject describe (DescribeOption... options) {
		if (options == null || options.length == 0) {
			return JsonObject.Blank;
		}
		
		Map<DescribeOption.Option, DescribeOption> opts = DescribeUtils.toMap (options);
		
		JsonObject describe = new JsonObject ();
		
		if (opts.containsKey (DescribeOption.Option.info)) {
			describe.set (ConfigKeys.Name, Json.getString (descriptor, ConfigKeys.Name));
			describe.set (ConfigKeys.Description, Json.getString (descriptor, ConfigKeys.Description));
			describe.set (ConfigKeys.Version, Json.getString (descriptor, ConfigKeys.Version));
		}
		
		if (opts.containsKey (DescribeOption.Option.keys)) {
			JsonObject okeys = keys.toJson ().duplicate ();
			okeys.remove (KeyPair.Fields.SecretKey);
			describe.set (DescribeOption.Option.keys.name (), okeys);
		}
		
		if (instanceDescriber != null && opts.containsKey (DescribeOption.Option.hardware)) {
			describe.set (DescribeOption.Option.hardware.name (), instanceDescriber.describe ());
		}
		
		// plugins
		if (opts.containsKey (DescribeOption.Option.plugins)) {
			JsonArray aPlugins = new JsonArray ();
			describe.set (DescribeOption.Option.plugins.name (), aPlugins);
			
			Iterator<String> pNames = pluginsRegistry.getNames ();
			while (pNames.hasNext ()) {
				String pName = pNames.next ();
				Plugin plugin = pluginsRegistry.lockup (pName);
				JsonObject oPlugin = new JsonObject ();
				oPlugin.set (ConfigKeys.Namespace, plugin.getNamespace ());
				oPlugin.set (ConfigKeys.Name, plugin.getName ());
				oPlugin.set (ConfigKeys.Description, plugin.getDescription ());
				oPlugin.set (ConfigKeys.Version, plugin.getVersion ());
				oPlugin.set (ConfigKeys.Vendor, plugin.getVendor ());
				aPlugins.add (oPlugin);
			}
		}
		
		// features
		if (opts.containsKey (DescribeOption.Option.features)) {
			JsonObject oFeatures = new JsonObject ();
			describe.set (DescribeOption.Option.features.name (), oFeatures);
			
			for (ServerFeature feature : features.values ()) {
				Class<?> fType = feature.type ();
				String name = null;
				if (customFeatures.containsKey (fType)) {
					name = customFeatures.get (fType);
				} else {
					name = feature.type ().getAnnotation (Feature.class).name ();
				}
				
				JsonArray aVendors = Json.getArray (oFeatures, name);
				if (aVendors == null) {
					aVendors = new JsonArray ();
					oFeatures.set (name, aVendors);
				}
				
				JsonObject oVendor = new JsonObject ();
				oVendor.set (Describe.Vendor, feature.implementor ().getVendor ());
				
				aVendors.add (oVendor);
			}
		}
		
		// spaces
		if (opts.containsKey (DescribeOption.Option.spaces)) {
			Collection<ApiSpace> spaces = spaces ();
			if (spaces != null && !spaces.isEmpty ()) {
				JsonArray aSpaces = new JsonArray ();
				describe.set (DescribeOption.Option.spaces.name (), aSpaces);
				for (ApiSpace space : spaces) {
					aSpaces.add (space.describe (options));
				}
			}
		}
		
		return describe;
	}
	
	@Override
	public KeyPair getKeys () {
		return keys;
	}
	
	@Override
	public ApiSpace space (String spaceNs) {
		if (spaces == null) {
			return null;
		}
		ApiSpace space = spaces.get (spaceNs);
		if (space == null) {
			throw new RuntimeException ("space " + spaceNs + " not found" );
		}
		return space;
	}
	
	@Override
	public void	addFeature (ServerFeature feature) {
		
		String featureId = feature.id ();
		
		if (Lang.isNullOrEmpty (featureId)) {
			Feature aFeature = feature.type ().getAnnotation (Feature.class);
			if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
				throw new FeatureNotFoundException ("feature " + feature.type ().getSimpleName () + " not registered in this instance");
			} 
			featureId = aFeature.name ();
		} else {
			customFeatures.put (feature.type (), featureId);
		}

		features.put (featureId + FeatureProtocol + feature.provider (), feature);
	}

	@Override
	public Object getFeature (ApiSpace space, Class<?> type, String name) {
		
		String featureId = customFeatures.get (type);
		if (Lang.isNullOrEmpty (featureId)) {
			Feature aFeature = type.getAnnotation (Feature.class);
			if (aFeature == null) {
				throw new FeatureNotFoundException ("feature annotation not declared in type " + type.getName ());
			}
			featureId = aFeature.name ();
		}

		JsonObject oFeature = Json.getObject (space.getFeatures (), featureId);
		
		if (Json.isNullOrEmpty (oFeature)) {
			throw new FeatureNotFoundException ("feature " + featureId + " not available in space " + space.getNamespace ());
		} 
		
		String fName = name;
		// check if it's fature factory abc#alpha where abc is the feature name and alpha is what the application is looking for.
		if (fName.lastIndexOf (Lang.SHARP) > 0) {
			fName = fName.substring (0, fName.indexOf (Lang.SHARP));
		}
		JsonObject oProvider = Json.getObject (oFeature, fName);
		if (Json.isNullOrEmpty (oProvider)) {
			throw new FeatureNotFoundException ("feature provider " + name + " not available in space " + space.getNamespace ());
		} 
		
		String provider = Json.getString (oProvider, ApiSpace.Features.Provider);
		if (Lang.isNullOrEmpty (provider)) {
			throw new FeatureNotFoundException ("provider for feature " + featureId + Lang.SLASH + name +  " is missing");
		} 
		
		ServerFeature feature = features.get (featureId + FeatureProtocol + provider);
		if (feature == null) {
			throw new FeatureNotFoundException ("feature " + name + Lang.COLON + featureId + FeatureProtocol + provider + " not found");
		}
		return feature.get (space, name);
	}

	@Override
	public ClusterPeer getPeer () {
		return peer;
	}

	@Override
	public PluginsRegistry getPluginsRegistry () {
		return pluginsRegistry;
	}

	@Override
	public ApiInterceptor getInterceptor () {
		return interceptor;
	}
	
	@Override
	public MapProvider getMapProvider () {
		return mapProvider;
	}
	
	@Override
	public ApiServiceValidator getServiceValidator () {
		return serviceValidator;
	}

	@Override
	public ApiConsumerResolver getConsumerResolver (String name) {
		if (consumerResolvers == null) {
			return null;
		}
		return consumerResolvers.get (name);
	}
	@Override
	public void addConsumerResolver (ApiConsumerResolver consumerResolver) {
		ApiConsumerResolverAnnotation ann = consumerResolver.getClass ().getAnnotation (ApiConsumerResolverAnnotation.class);
		if (ann == null) {
			return;
		}
		String name = ann.name ();
		consumerResolvers.put (name, consumerResolver);
	}
	
	@Override
	public ApiRequestSigner getRequestSigner () {
		return requestSigner;
	}	
	
	@Override
	public ServerRequestTracker getRequestTracker (String id) {
		ServerRequestTracker tracker = BlankApiRequestTracker.Instance;
		if (Lang.isNullOrEmpty (id)) {
			return tracker;
		}
		
		id = id.toLowerCase ();
		
		if (requestTrackers.containsKey (id)) {
			return requestTrackers.get (id);
		}
		
		return tracker;
	}	
	
	@Override
	public ApiRequestVisitor getRequestVisitor () {
		return requestVisitor;
	}	
	
	@Override
	public KeyStoreManager getKeyStoreManager () {
		return keyStoreManager;
	}	
	
	@Override
	public void execute (final ApiRequest request, final ApiResponse response, CodeExecutor.Mode mode) {
		
		if (keys.expiryDate () != null && keys.expiryDate ().before (new Date ())) {
			sendError (response, ApiResponse.FORBIDDEN, "node keys expired");
			return;
		}
		
		ApiSpace 	space 	= null;
		Api 		api 	= null;
		
		try {
			if (!(request instanceof ContainerApiRequest)) {
				requestVisitor.visit ((AbstractApiRequest)request);
			}
			
			if (request.get (ApiRequest.Interceptors.Bypass) != null) {
				return;
			}
			
			if (request.get (ApiRequest.Interceptors.Response) != null) {
				JsonObject oResponse = (JsonObject)request.get (ApiRequest.Interceptors.Response);
				Status status = new ApiResponse.Status (Json.getInteger (oResponse, ApiResponse.Output.Status, 200));
				Object message = oResponse.get (ApiResponse.Output.Data);
				if (message == null) {
					message = status.getMessage ();
				}
				send (response, status, message);
				request.destroy ();
				return;
			}
			
			// is space resolved
			if (Lang.isNullOrEmpty (request.getSpace ())) {
				sendError (response, ApiResponse.NOT_FOUND, "can't resolve space from request");
				request.destroy ();
				return;
			}
			
			if (Lang.isNullOrEmpty (request.getApi ())) {
				sendError (response, ApiResponse.NOT_FOUND, "can't resolve api namespace from request");
				request.destroy ();
				return;
			}
			
			space = space (request.getSpace ());
			
			ApiResponse.Status 	notFoundStatus 	= null;
			String 				notFoundMessage = null;
			
			if (space == null) {
				notFoundStatus 	= ApiResponse.NOT_FOUND;
				notFoundMessage = "space " + request.getSpace () + " not found";
			} else if (!space.isStarted ()) {
				notFoundStatus 	= ApiResponse.SERVICE_UNAVAILABLE;
				notFoundMessage = "space " + request.getSpace () + " is not available";
			}
			
			if (notFoundStatus != null) {
				sendError (response, notFoundStatus, notFoundMessage);
				request.destroy ();
				return;
			}
			
			api = space.api (request.getApi ());
			
			if (api == null) {
				notFoundStatus 	= ApiResponse.NOT_FOUND;
				notFoundMessage = "api " + request.getApi () + " not found";
			} else if (api.status () != ApiStatus.Running) {
				notFoundStatus 	= ApiResponse.SERVICE_UNAVAILABLE;
				notFoundMessage = "api " + request.getApi () + " stopped or paused";
			}
			
			if (notFoundStatus != null) {
				sendError (response, notFoundStatus, notFoundMessage);
				request.destroy ();
				return;
			}
		} catch (Exception ex) {
			tracer.log (Tracer.Level.Error, Lang.BLANK, ex);
			request.destroy ();
			sendError (response, ApiResponse.BAD_REQUEST, ex.getMessage ());
			return;
		} 
			
		final Api 		fApi = api;
		try {
			space.executor ().execute (new Callable<Void> () {
				@Override
				public Void call () {
					if (Thread.currentThread () instanceof SpaceThread) {
						final SpaceThread currentThread = (SpaceThread)Thread.currentThread ();
				        currentThread.setRequest (request);
					}
			        try {
						interceptor.intercept (fApi, request, response);	
			        } finally {
						if (Thread.currentThread () instanceof SpaceThread) {
				        	((SpaceThread)Thread.currentThread ()).setRequest (null);
						}
			        }
			        return null;
				}
			}, mode);
		} catch (CodeExecutorException aaee) {
			Throwable e = aaee.getCause ();
			
			String error = "Generic";
			ApiResponse.Status status = ApiResponse.BAD_REQUEST;
			
	   		if (e.getClass ().equals (CancellationException.class)) {
	   			status = ApiResponse.INSUFFICIENT_SPACE_ON_RESOURCE;
	   			error = "Cancellation";
	   		} else if (e.getClass ().equals (TimeoutException.class)) {
	   			status = ApiResponse.REQUEST_TIMEOUT;
	   			error = "Timeout";
	   		} 
			
	   		tracer.log (Tracer.Level.Error, 
				"\tCallback-OnError: ThreadGroup [" + Thread.currentThread ().getThreadGroup ().getName () + 
				"], Thread [" + Thread.currentThread ().getName () + 
				"], " + error + " Error: " + e.getMessage (), 
				e
			);
	   		sendError (response, status, error + " Error | " + e.getMessage ());
			request.destroy ();
		}
		
	}

	@Override
	public String message (String lang, String key, Object... args) {
		if (lang == null) {
			lang = ConfigKeys.DefaultLang;
		}
		JsonObject msgset = Json.getObject (messages, key);
		if (msgset == null || msgset.isEmpty ()) {
			return key;
		}
		String msg = Json.getString (msgset, lang);
		if (msg == null) {
			msg = Json.getString (msgset, ConfigKeys.DefaultLang);
		}
		if (msg == null) {
			return key;
		}
		return MessageFormat.format (msg, args);
	}
	
	@Override
	public ApiMediaProcessorRegistry getMediaProcessorRegistry () {
		return mediaProcessorRegistry;
	}

	@Override
	public InstanceDescriber getInstanceDescriber () {
		return instanceDescriber;
	}
	
	@Override
	public Collection<ApiSpace> spaces () {
		if (spaces == null) {
			return null;
		}
		return spaces.values ();
	}

	@Override
	public void setInterceptor (ApiInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public void setServiceValidator (ApiServiceValidator serviceValidator) {
		this.serviceValidator = serviceValidator;
	}

	@Override
	public void setRequestSigner (ApiRequestSigner requestSigner) {
		this.requestSigner = requestSigner;
	}

	@Override
	public void addRequestTracker (String id, ServerRequestTracker requestTracker) {
		requestTrackers.put (id.toLowerCase (), requestTracker);
	}	

	@Override
	public void setRequestVisitor (ApiRequestVisitor requestVisitor) {
		this.requestVisitor = requestVisitor;
	}	

	@Override
	public void setKeyStoreManager (KeyStoreManager keyStoreManager) {
		this.keyStoreManager = keyStoreManager;
	}	

	@Override
	public void setMapProvider (MapProvider mapProvider) {
		this.mapProvider = mapProvider;
	}	

	@Override
	public void setInstanceDescriber (InstanceDescriber instanceDescriber) {
		this.instanceDescriber = instanceDescriber;
	}

	@Override
	public Tracer tracer () {
		if (tracer == null) {
			return NoTracing.Instance;
		}
		return tracer;
	}
	
	public void setPeer (ClusterPeer peer) {
		this.peer = peer;
	}	
	public void setPluginsRegistry (PluginsRegistry pluginsRegistry) {
		this.pluginsRegistry = pluginsRegistry;
	}
	
	protected void addSpace (ApiSpace space) {
		spaces.put (space.getNamespace (), space);
	}

	private void sendError (ApiResponse response, Status status, String message) {
		if (response instanceof ContainerApiResponse) {
			((ContainerApiResponse)response).setException (
				new ApiServiceExecutionException (message).status (status)
			);
		} else {
			response.set (ApiHeaders.ContentType, ApiContentTypes.Json);
			response.error (status, message);
			try {
				response.write (response.getError ());
				ApiUtils.logError (response, tracer);
			} catch (IOException e) {
				tracer.log (Tracer.Level.Error, Lang.BLANK, e);
			} finally {
				try { response.close (); } catch (Exception ex) { }
			}
		}
	}
	
	private void send (ApiResponse response, Status status, Object message) {
		if (response instanceof ContainerApiResponse && status.getCode () > 399) {
			((ContainerApiResponse)response).setException (
				new ApiServiceExecutionException (String.valueOf (message)).status (status)
			);
			return;
		} 
		
		response.setStatus (status);
		response.set (ApiHeaders.ContentType, ApiContentTypes.Json);
		
		try {
			response.write (message);
		} catch (IOException e) {
			tracer.log (Tracer.Level.Error, Lang.BLANK, e);
		} finally {
			try { response.close (); } catch (Exception ex) { }
		}
	}
	
}

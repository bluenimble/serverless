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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.ValueHolder;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiResourcesManager;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiServicesManager;
import com.bluenimble.platform.api.ApiServicesManager.Selector;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiSpi;
import com.bluenimble.platform.api.ApiStatus;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.CodeExecutor;
import com.bluenimble.platform.api.DescribeOption;
import com.bluenimble.platform.api.impls.spis.DefaultApiSpi;
import com.bluenimble.platform.api.media.ApiMediaProcessor;
import com.bluenimble.platform.api.media.MediaTypeUtils;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.api.tracing.impls.NoTracing;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.ApiServiceValidatorException;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.reflect.BeanUtils;
import com.bluenimble.platform.regex.WildcardCompiler;
import com.bluenimble.platform.regex.WildcardMatcher;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.impls.ApiClassLoader;
import com.bluenimble.platform.server.utils.ConfigKeys;
import com.bluenimble.platform.server.utils.DescribeUtils;
import com.bluenimble.platform.server.utils.InstallUtils;

public class ApiImpl implements Api {
	
	private static final long serialVersionUID = -6354828571701974927L;

	interface Describe {
		String Space 			= "space";	
		String Services 		= "services";	
		
		String Failure			= "failure";
		String FailedServices	= "failedServices";
		String FailureWhy		= "why";
		String FailureTrace		= "trace";
	}
	
	private Tracer tracer = NoTracing.Instance;

	private static final ApiSpi DefaultApiSpi = new DefaultApiSpi ();
	
	ApiSpaceImpl space;
	
	private ApiClassLoader classLoader;
	
	private JsonObject descriptor;
	
	private Map<String, JsonObject> i18n = new HashMap<String, JsonObject> ();
	
	private ApiSpi spi;
	
	private File home;

	private ApiServiceValidator		serviceValidator;
	private ApiResourcesManager 	resourcesManager;
	private ApiServicesManager 		servicesManager;

	private JsonObject 				failure;
	
	private ApiStatus 				status;
	
	private Map<String, Object>		helpers;
		
	public ApiImpl (ApiSpaceImpl space, File home) {
		this.space 	= space;
		this.home 	= home;
		
		init ();
	}
	
	private void init () {
		
		readDescriptor ();
		
		setStatus (Json.getString (this.descriptor, Spec.Status));
		
		String namespace = getNamespace ();

		if (!InstallUtils.isValidApiNs (namespace)) {
			failed ("Invalid api namespace " + namespace);
		}
		
		// load resources manager
		resourcesManager 	= new DefaultApiResourcesManager ();
		try {
			resourcesManager.load (this);
		} catch (Exception ex) {
			failed (ex);
		} 
		
		// load services manager
		servicesManager 	= new DefaultApiServicesManager (space.getServer ().getPluginsRegistry ());
		try {
			servicesManager.load (this);
		} catch (Exception ex) {
			failed (ex);
		} 
		
		if (status == null) {
			setStatus (ApiStatus.Stopped, false);
		}
				
	}
	
	private void readDescriptor () {
		
		File fDescriptor = new File (home, ConfigKeys.Descriptor.Api);
		if (fDescriptor.exists () && fDescriptor.isFile ()) {
			try {
				this.descriptor = Json.load (new File (home, ConfigKeys.Descriptor.Api));
			} catch (Exception ex) {
				failed (ex);
			} 
		}
		
		if (this.descriptor == null) {
			String namespace = getNamespace ();
			if (Lang.isNullOrEmpty (namespace)) {
				namespace = Lang.UUID (10);
			}
			this.descriptor = (JsonObject)new JsonObject ().set (Api.Spec.Namespace, namespace);
		} 
		
	}
	
	public void start (boolean pause) {
		
		// create classLoader
		try {
			if (new File (home, ConfigKeys.Folders.Lib).exists () || 
					!Json.isNullOrEmpty (Json.getArray (descriptor, ConfigKeys.Classpath)) ||
					!Json.isNullOrEmpty (Json.getArray (descriptor, ConfigKeys.Dependencies))
			) {
				
				ClassLoader [] clds = null;
				
				JsonArray aDependencies = Json.getArray (descriptor, ConfigKeys.Dependencies); 
				if (!Json.isNullOrEmpty (aDependencies)) {
					clds = new ClassLoader [aDependencies.count ()];
					for (int i = 0; i < aDependencies.count (); i++) {
						clds [i] = space.server.getPluginsRegistry ().find (String.valueOf (aDependencies.get (i)));
					}
				}
				
				this.classLoader = new ApiClassLoader (
					space.getNamespace () + Lang.DOT + getNamespace (), 
					InstallUtils.toUrls (home, Json.getArray (descriptor, ConfigKeys.Classpath)),
					clds
				);
			}
		} catch (Exception ex) {
			failed (ex);
		} 
		
		// start resources manager
		try {
			resourcesManager.onStart ();
		} catch (Exception ex) {
			failed (ex);
		} 

		// load messages
		try {
			loadI18n (); 
		} catch (Exception ex) {
			failed (ex);
		} 

		// create spi
		try {
			spi = (ApiSpi)BeanUtils.create (this.getClassLoader (), Json.getObject (descriptor, ConfigKeys.Spi), space.getServer ().getPluginsRegistry ());
		} catch (Exception ex) {
			failed (ex);
		} 
		
		if (spi == null) {
			spi = DefaultApiSpi;
		}
		
		space.tracer ().log (Tracer.Level.Info, "\t  Namespace: {0}", getNamespace ());
		space.tracer ().log (Tracer.Level.Info, "\t       Name: {0}", getName ());
		if (getDescription () != null) {
			space.tracer ().log (Tracer.Level.Info, "\tDescription: {0}", getDescription ());
		} 
		
		// set validator
		JsonObject oValidator = Json.getObject (descriptor, ConfigKeys.ServiceValidator);
		if (!Json.isNullOrEmpty (oValidator)) {
			try {
				serviceValidator = (ApiServiceValidator)BeanUtils.create (this.getClassLoader (), oValidator, space.getServer ().getPluginsRegistry ());
			} catch (Exception ex) {
				failed (ex);
			} 
		}
		if (serviceValidator == null) {
			serviceValidator = space.server.getServiceValidator ();
		}

		// init tracer
		JsonObject oTracer = Json.getObject (descriptor, ConfigKeys.Tracer);
		if (!Json.isNullOrEmpty (oTracer)) {
			try {
				tracer = (Tracer)BeanUtils.create (this.getClassLoader (), oTracer, space.getServer ().getPluginsRegistry ());
			} catch (Exception ex) {
				failed (ex);
			} 
		}
		tracer.onInstall (this);
		space.tracer ().log (Tracer.Level.Info, "\t     Tracer: {0}", tracer.getClass ().getSimpleName ());
		
		ApiContext context = new DefaultApiContext ();
		
		try {
			// start spi
			try {
				
				// notify api start
				space.server.getPluginsRegistry ().onEvent (Event.Start, this);
				
				// start api
				spi.onStart (this, context);
				
			} catch (Exception ex) {
				failed (ex);
			} 
			
			// start services manager
			try {
				servicesManager.onStart (context);
			} catch (Exception ex) {
				failed (ex);
			} 
			
		} finally {
			context.recycle ();
		}
		
		// update/save status
		if (!ApiStatus.Failed.equals (status)) {
			
			setStatus (pause ? ApiStatus.Paused : ApiStatus.Running, true);
			
			if (Lang.isDebugMode ()) {
				final StringBuilder sb = new StringBuilder (space.getNamespace () + Lang.SLASH + getNamespace () + " available services\n");
				servicesManager.list (new Selector () {
					@Override
					public boolean select (ApiService service) {
						sb.append (Lang.TAB)
							.append (Lang.PARENTH_OPEN).append (service.status ().name ().substring (0, 1)).append (Lang.PARENTH_CLOSE).append (Lang.SPACE)
							.append (service.getVerb ()).append (Lang.COLON).append (Json.getString (service.toJson (), ApiService.Spec.Endpoint));
							if (ApiStatus.Failed.equals (service.status ())) {
								sb.append (Lang.ENDLN).append (service.getFailure ().toString (2));
							}
							sb.append (Lang.ENDLN);
						return false;
					}
				});
				space.tracer ().log (Tracer.Level.Info, sb);
				sb.setLength (0);
			}
		}
		
		space.tracer ().log (Tracer.Level.Info, "\tapi {0} {1}", getNamespace (), status);

	}

	public void stop (boolean saveStatus) {
		
		// notify plugins
		try {
			space.server.getPluginsRegistry ().onEvent (Event.Stop, this);
		} catch (PluginRegistryException ex) {
			space.tracer ().log (Tracer.Level.Error, "notify plugins caused an error", ex);
		}

		ApiContext context = new DefaultApiContext ();
		
		// call services manager onStop 
		try {
			servicesManager.onStop (context);
		} catch (Exception ex) {
			space.tracer ().log (Tracer.Level.Error, "ServiceManager.stop casued an error", ex);
		}
	
		// call spi onStop
		if (spi != null) {
			try {
				spi.onStop (this, context);
			} catch (Exception ex) {
				space.tracer ().log (Tracer.Level.Error, "ApiSpi.onStop casued an error", ex);
			} 
		}
		
		// recycle context
		context.recycle ();
		
		// clear messages
		if (!i18n.isEmpty ()) {
			Iterator<String> langs = i18n.keySet ().iterator ();
			while (langs.hasNext ()) {
				i18n.get (langs.next ()).clear ();
			}
		}
		i18n.clear ();
		
		if (classLoader != null) {
			try {
				classLoader.clear ();
			} catch (IOException e) {
				space.tracer ().log (Tracer.Level.Error, Lang.BLANK, e);
			}
		}

		// update/save status
		setStatus (ApiStatus.Stopped, saveStatus);

		space.tracer ().log (Tracer.Level.Info, "\tapi {0} {1}", getNamespace (), status);
		
		tracer.onShutdown (this);
		
	}

	@Override
	public Object getHelper (String key) {
		if (helpers == null) {
			return null;
		}
		return helpers.get (key);
	}

	@Override
	public void setHelper (String key, Object helper) {
		if (helpers == null) {
			helpers = new ConcurrentHashMap<String, Object> ();
		}
		helpers.put (key, helper);
	}

	@Override
	public ApiResourcesManager getResourcesManager () {
		return resourcesManager;
	}

	@Override
	public ApiServicesManager getServicesManager () {
		return servicesManager;
	}

	@Override
	public ApiServiceValidator getServiceValidator () {
		return serviceValidator;
	}

	@Override
	public Tracer tracer () {
		if (tracer == null) {
			return NoTracing.Instance;
		}
		return tracer;
	}
	
	@Override
	public ApiSpi getSpi () {
		return spi;
	}

	@Override
	public String getNamespace () {
		return Json.getString (descriptor, Spec.Namespace);
	}

	@Override
	public String getName () {
		return Json.getString (descriptor, Spec.Name, getNamespace ());
	}

	@Override
	public String getDescription () {
		return Json.getString (descriptor, Spec.Description);
	}

	@Override
	public ApiSpace space () {
		return space;
	}

	@Override
	public JsonObject getRelease () {
		return Json.getObject (descriptor, Spec.Release);
	}

	@Override
	public JsonObject getRuntime () {
		return Json.getObject (descriptor, Spec.Runtime.class.getSimpleName ().toLowerCase ());
	}

	@Override
	public JsonObject getFeatures () {
		return Json.getObject (descriptor, Spec.Features);
	}

	@Override
	public JsonObject getMedia () {
		return Json.getObject (descriptor, Spec.Media.class.getSimpleName ().toLowerCase ());
	}

	@Override
	public JsonObject getSecurity () {
		return Json.getObject (descriptor, Spec.Security.class.getSimpleName ().toLowerCase ());
	}

	@Override
	public JsonObject getTracking () {
		return Json.getObject (descriptor, Spec.Tracking.class.getSimpleName ().toLowerCase ());
	}

	@Override
	public JsonObject getSpiDef () {
		return Json.getObject (descriptor, Spec.Spi.class.getSimpleName ().toLowerCase ());
	}

	@Override
	public String message (String lang, String key, Object... args) {
		if (lang == null) {
			lang = ConfigKeys.DefaultLang;
		}
		
		JsonObject langI18n = i18n.get (lang);
		
		if (langI18n == null || langI18n.isEmpty ()) {
			langI18n = i18n.get (ConfigKeys.DefaultLang);
		}
		
		if (langI18n == null || langI18n.isEmpty ()) {
			return space.getServer ().message (lang, key, args);
		}
		
		String msg = Json.getString (langI18n, key);
		
		if (msg == null) {
			return space.getServer ().message (lang, key, args);
		}
		return MessageFormat.format (msg, args);
	}

	@Override
	public JsonObject i18n (String lang) {
		if (lang == null) {
			lang = ConfigKeys.DefaultLang;
		}
		
		JsonObject langI18n = i18n.get (lang);
		
		if (langI18n == null || langI18n.isEmpty ()) {
			return i18n.get (ConfigKeys.DefaultLang);
		}
		
		return langI18n;
	}

	@Override
	public ApiMediaProcessor lockupMediaProcessor (ApiRequest request, ApiService service) {
		
		String defaultContentType = Json.getString (getMedia (), Api.Spec.Media.Default, ApiContentTypes.Json);
		
		String accept = (String)request.get (ApiHeaders.Accept, Scope.Header);
		
		space.tracer ().log (Level.Info, ApiHeaders.Accept + " header: {0}", accept);
		
		if (Lang.isNullOrEmpty (accept)) {
			accept = defaultContentType;
		}
		
		accept = accept.toLowerCase ();
		
		// check if accept is in service media spec and get the processor

		String processor = (String)Json.find (service.getMedia (), accept, ApiService.Spec.Media.Processor);
		
		space.tracer ().log (Level.Info, "Processor: {0}", processor);
		
		if (!Lang.isNullOrEmpty (processor)) {
			space.tracer ().log (Level.Info, "Processor found, Set SelectedMedia to {0}", accept);
			request.set (ApiRequest.SelectedMedia, accept);
			return space.getServer ().getMediaProcessorRegistry ().lockup (processor);
		}
		
		// if service has no media spec, get default
		if (Json.isNullOrEmpty (service.getMedia ())) {
			space.tracer ().log (Level.Info, "service.getMedia not found, Set SelectedMedia to {0}", accept);
			request.set (ApiRequest.SelectedMedia, accept);
			return space.getServer ().getMediaProcessorRegistry ().getDefault ();
		}
		
		// find a best match for accept in service media spec and get the processor
		@SuppressWarnings("unchecked")
		String [] candidates = Lang.toArray (
			(Set<String>)service.getMedia ().keySet (), 
			null
		);
		
		String bestMatch = MediaTypeUtils.bestMatch (candidates, accept);
		if (bestMatch != null) {
			accept = bestMatch;
		}
		
		processor = (String)Json.find (service.getMedia (), accept, ApiService.Spec.Media.Processor);
		if (!Lang.isNullOrEmpty (processor)) {
			request.set (ApiRequest.SelectedMedia, accept);
			return space.getServer ().getMediaProcessorRegistry ().lockup (processor);
		}
		
		// no processor found in service media spec
		request.set (ApiRequest.SelectedMedia, accept);
		return space.getServer ().getMediaProcessorRegistry ().getDefault ();
	
	}
		
	@Override
	public void validate (ApiConsumer consumer, JsonObject spec, ApiRequest request)
			throws ApiServiceValidatorException {
		space.getServer ().getServiceValidator ().validate (this, spec, consumer, request);
	}

	@Override
	public ApiOutput call (ApiRequest request) throws ApiServiceExecutionException {
		ContainerApiResponse	response 	= new ContainerApiResponse (request.getId ());
		
		space.getServer ().execute (request, response, CodeExecutor.Mode.Sync);
		
		if (response.getException () != null) {
			throw response.getException ();
		}
		
		return (ApiOutput)request.get (ApiRequest.Output);
	}

	@Override
	public JsonObject getFailure () {
		return failure;
	}

	@Override
	public ApiStatus status () {
		return status;
	}
	
	@Override
	public JsonObject describe (final DescribeOption... options) {
		if (options == null || options.length == 0) {
			return JsonObject.Blank;
		}
		
		final Map<DescribeOption.Option, DescribeOption> opts = DescribeUtils.toMap (options);
		
		JsonObject describe = new JsonObject ();
		
		if (opts.containsKey (DescribeOption.Option.info)) {
			describe.set (Api.Spec.Namespace, getNamespace ());
			describe.set (Describe.Space, space.getNamespace ());
			describe.set (Api.Spec.Name, getName ());
			describe.set (Api.Spec.Description, getDescription ());
			describe.set (Api.Spec.Status, status ().name ());
			if (failure != null) {
				describe.set (
					Describe.Failure, 
					failure
				);
			}
			describe.set (Api.Spec.Release, getRelease ());
		}
		
		if (getSecurity () != null && opts.containsKey (DescribeOption.Option.security)) {
			describe.set (Api.Spec.Security.class.getSimpleName ().toLowerCase (), getSecurity ().duplicate ());
		}
		
		if (getTracking () != null && opts.containsKey (DescribeOption.Option.tracking)) {
			describe.set (Api.Spec.Tracking.class.getSimpleName ().toLowerCase (), getTracking ().duplicate ());
		}
		
		if (getFeatures () != null && opts.containsKey (DescribeOption.Option.features)) {
			describe.set (Api.Spec.Features, getFeatures ().duplicate ());
		}
		
		if (getRuntime () != null && opts.containsKey (DescribeOption.Option.runtime)) {
			describe.set (Api.Spec.Runtime.class.getSimpleName ().toLowerCase (), getRuntime ().duplicate ());
		}
		
		if (servicesManager.isEmpty (null)) {
			return describe;
		}
		
		final JsonObject failedServices = new JsonObject ();
		describe.set (Describe.FailedServices, failedServices);
		
		JsonArray aServices = new JsonArray ();
		describe.set (Describe.Services, aServices);
		
		final JsonArray fServices = aServices;
		
		final ValueHolder<Integer> apiMarkers = new ValueHolder<Integer> (0);
		
		ValueHolder<Integer> failed = new ValueHolder<Integer> (0);
		
		servicesManager.list (new Selector () {
			@Override
			public boolean select (ApiService service) {
				if (opts.containsKey (DescribeOption.Option.services)) {
					DescribeOption opt = opts.get (DescribeOption.Option.services);
					
					JsonObject jService = service.toJson ();
					
					JsonObject sDesc = jService;
					
					if (!opt.isVerbose ()) {
						sDesc = new JsonObject ();
						sDesc.set (ApiService.Spec.Name, jService.get (ApiService.Spec.Name));
						sDesc.set (ApiService.Spec.Endpoint, jService.get (ApiService.Spec.Endpoint));
						sDesc.set (ApiService.Spec.Verb, jService.get (ApiService.Spec.Verb));
						sDesc.set (ApiService.Spec.Status, jService.get (ApiService.Spec.Status));
						sDesc.set (ApiService.Spec.Security.class.getSimpleName ().toLowerCase (), service.getSecurity ());
						
						JsonArray aMarkers = Json.getArray (jService, Api.Spec.Markers);
						int markers = aMarkers != null ? aMarkers.size () : 0;
						
						apiMarkers.set (apiMarkers.get () + markers);
						
						if (markers > 0) {
							sDesc.set (Api.Spec.Markers, markers);
						}
					}
					
					fServices.add (sDesc);
				}
				if (ApiStatus.Failed.equals (service.status ())) {
					if (opts.containsKey (DescribeOption.Option.failed)) {
						failedServices.put (
							service.getVerb ().name () + Lang.SPACE + Json.getString (service.toJson (), ApiService.Spec.Endpoint), 
							service.getFailure ()
						);
					} else {
						failed.set (failed.get () + 1);
					}
				}
				return false;
			}
		});
		
		describe.set (Api.Spec.Markers, apiMarkers.get ());
		
		if (fServices.isEmpty ()) {
			describe.remove (Describe.Services);
		}
		
		if (failedServices.isEmpty ()) {
			if (failed.get () > 0) {
				describe.set (Describe.FailedServices, failed.get ());
			} else {
				describe.remove (Describe.FailedServices);
			}
		}
		
		return describe;		
	}

	@Override
	public ClassLoader getClassLoader () {
		return classLoader == null ? Api.class.getClassLoader () : classLoader;
	}

	public ApiService lockup (ApiRequest request) {
		if (request == null) {
			return null;
		}
		
		ApiVerb verb = request.getVerb ();
		if (verb == null) {
			verb = ApiVerb.GET;
		}
		
		ApiService service = lockup (verb, request.getResource ());
		resolveParameters (service, request);
		return service;
	}

	protected ApiService lockup (final ApiVerb verb, String [] endpoint) {
		
		if (servicesManager.isEmpty (verb)) {
			return null;
		}
		
		String path = Lang.SLASH;
		if (endpoint != null && endpoint.length > 0) {
			path += Lang.join (endpoint, Lang.SLASH);
		}

		ApiService service = servicesManager.get (verb, path);
		if (service != null) {
			return service;
		}		
		
		final String wildcard = path;
		
		final ValueHolder<ApiService> holder = new ValueHolder<ApiService> ();
		
		servicesManager.list (new Selector () {
			@Override
			public boolean select (ApiService service) {
				WildcardMatcher matcher = new WildcardMatcher (WildcardCompiler.compile (service.getEndpoint ()), wildcard);
				if (matcher.find () && service.getVerb ().equals (verb)) {
					holder.set (service);
					return true;
				}
				return false;
			}
		});
		
		return holder.get ();
	}

	private void resolveParameters (ApiService service, ApiRequest request) {
		
		if (service == null) {
			return;
		}
		
		JsonObject accessors = ((ApiServiceImpl)service).accessors ();
		if (accessors == null || accessors.isEmpty ()) {
			return;
		}
		
		String [] resource = request.getResource ();
		if (resource == null) {
			return;
		}
		
		Iterator<String> keys = accessors.keys ();
		while (keys.hasNext ()) {
			String p = keys.next ();
			JsonObject a = Json.getObject (accessors, p);
			int 	index 	= Json.getInteger (a, ApiServiceImpl.SpecExt.Index, 0);
			boolean unary 	= Json.getBoolean (a, ApiServiceImpl.SpecExt.Unary, true);
			
			if (!unary) {
				request.set (p, Lang.join (Lang.moveLeft (resource, index), Lang.SLASH));
				return;
			}
			
			request.set (p, resource [index]);
		}
	}
	
	private void failed (String message) {
		space.server.tracer ().log (Tracer.Level.Error, message);
		failure = (JsonObject)new JsonObject ().set (ApiResponse.Error.Message, message);
		setStatus (ApiStatus.Failed, false);
	}
	private void failed (Exception ex) {
		space.server.tracer ().log (Tracer.Level.Error, Lang.BLANK, ex);
		failure = Lang.toError (ex);
		setStatus (ApiStatus.Failed, false);
	}
	
	public File getHome () {
		return home;
	}

	public void setStatus (ApiStatus status, boolean save) {
		if (descriptor == null) {
			return;
		}
		this.status = status;
		descriptor.set (Api.Spec.Status, status.name ());
		if (save) {
			space.statusManager.update (this, status);
		}
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
	
	public JsonObject getDescriptor () {
		return descriptor;
	}
	
	private void loadI18n () throws Exception {
		File messagesFolder = new File (new File (home, ConfigKeys.Folders.Resources), ConfigKeys.Folders.Messages);
		if (messagesFolder.exists ()) {
			File [] fMessages = messagesFolder.listFiles (new FileFilter () {
				@Override
				public boolean accept (File file) {
					return file.isFile () && file.getName ().endsWith (ConfigKeys.JsonExt);
				}
			});
			if (fMessages != null && fMessages.length > 0) {
				for (File mFile : fMessages) {
					JsonObject messages = Json.load (mFile);
					Iterator<String> keys = messages.keys ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						JsonObject langsMessages = Json.getObject (messages, key);
						Iterator<String> langs = langsMessages.keys ();
						while (langs.hasNext ()) {
							String lang = langs.next ();
							JsonObject langI18n = i18n.get (lang);
							if (langI18n == null) {
								langI18n = new JsonObject ();
								i18n.put (lang, langI18n);
							}
							langI18n.put (key, langsMessages.get (lang));
						}
					}
				}
			}
		}
	}
	
	void clear () {
		space = null;
		classLoader = null;
		descriptor.clear (); 
		descriptor = null;
		i18n = null;
		spi = null;
		resourcesManager = null;
		servicesManager = null;
		failure = null;
		status = null;
		
		if (helpers != null) {
			helpers.clear ();
		}
		helpers = null;
	}

}

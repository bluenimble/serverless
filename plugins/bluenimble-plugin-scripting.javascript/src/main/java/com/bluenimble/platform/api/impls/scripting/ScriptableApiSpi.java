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
package com.bluenimble.platform.api.impls.scripting;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiSpi;
import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.ScriptContext;
import com.bluenimble.platform.scripting.ScriptingEngine;
import com.bluenimble.platform.scripting.ScriptingEngine.Supported;

import jdk.nashorn.api.scripting.NashornException;

import com.bluenimble.platform.scripting.ScriptingEngineException;

@SuppressWarnings("restriction")
public class ScriptableApiSpi implements ApiSpi {

	private static final long serialVersionUID = 1507561751507700274L;
	
	interface Paths {
		String Scripting 	= "scripting";
			String Api 			= "api";
			String Spi 			= "spi";
	}

	interface Functions {
		String OnStart 		= "onStart";
		String OnStop 		= "onStop";
		
		String OnRequest 	= "onRequest";
		String OnService 	= "onService";
		String OnExecute 	= "onExecute";
		String AfterExecute = "afterExecute";

		String OnError 		= "onError";
		String FindConsumer = "findConsumer";
	}
	
	private static final String ConsumerSet 		= "set";
	
	@Override
	public void onStart (Api api, ApiContext context) throws ApiManagementException {
		String _function = Json.getString (api.getSpiDef (), Api.Spec.Spi.Function);
		if (Lang.isNullOrEmpty (_function)) {
			throw new ApiManagementException ("function not defined in " + Api.Spec.Spi.class.getSimpleName ().toLowerCase ());
		}
		String [] path = Lang.split (_function, Lang.SLASH);
		
		ApiResource rScript = null;
		try {
			rScript = api.getResourcesManager ().get (path);
		} catch (ApiResourcesManagerException ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		}
		
		if (rScript == null) {
			throw new ApiManagementException ("function '" + Lang.join (path, Lang.SLASH) + "' not found");
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, context);
		
		// create the spi
		Object jsSpi = null;
		try {
			jsSpi = engine.eval (Supported.Javascript, api, rScript, ScriptContext.Empty);
		} catch (Exception ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		}
		
		if (jsSpi == null) {
			throw new ApiManagementException ("function returned an undefined object");
		}

		// create the api object
		Object jsApi = null;
		try {
			jsApi = engine.invoke (null, Api.class.getSimpleName (), api);
		} catch (Exception ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		}
		
		if (jsApi == null) {
			throw new ApiManagementException ("can't create 'api spec' js object");
		}
		
		api.setHelper (SpecAndSpiPair.Name, new SpecAndSpiPair (jsApi, jsSpi));
		
		if (!engine.has (jsSpi, Functions.OnStart)) {
			return;
		}
		
		// invoke onStart
		try {
			engine.invoke (jsSpi, Functions.OnStart, jsApi, context);
		} catch (ScriptingEngineException ex) {
			ex.setScript (_function);
			throw new ApiManagementException (ex.getMessage (), ex);
		}		
	}

	@Override
	public void onStop (Api api, ApiContext context) throws ApiManagementException {
		SpecAndSpiPair ssp = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (ssp == null) {
			return;
		}

		Object spi = ssp.spi ();
		if (spi == null) {
			return;
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, context);
		if (!engine.has (spi, Functions.OnStop)) {
			return;
		}
		
		Object jsApi = ((SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name)).spec ();
		if (jsApi == null) {
			return;
		}		

		// invoke onStop
		try {
			engine.invoke (spi, Functions.OnStop, jsApi, context);
		} catch (Exception ex) {
			api.tracer ().log (Level.Error, Lang.BLANK, ex);
		}		
	}

	@Override
	public void onRequest (Api api, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		SpecAndSpiPair ssp = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (ssp == null) {
			return;
		}

		Object spi = ssp.spi ();
		if (spi == null) {
			return;
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, request);
		if (!engine.has (spi, Functions.OnRequest)) {
			return;
		}
		
		Object jsApi = ((SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name)).spec ();
		if (jsApi == null) {
			throw new ApiServiceExecutionException ("api or spi not attached on Api OnStart");
		}		

		// invoke onRequest
		try {
			engine.invoke (spi, Functions.OnRequest, jsApi, request, response);
		} catch (ScriptingEngineException ex) {
			ex.setScript (Json.getString (api.getSpiDef (), Api.Spec.Spi.Function));
			Throwable cause = ex;
			if (ex.getCause () != null) {
				cause = ex.getCause ();
			}
			ApiResponse.Status status = null;
			if (cause instanceof NashornException && cause.getCause () != null) {
				if (cause.getCause () instanceof ApiServiceExecutionException) {
					ApiServiceExecutionException see = (ApiServiceExecutionException)cause.getCause ();
					status = see.status ();
				}
			}
			throw new ApiServiceExecutionException (cause.getMessage (), cause).status (status);
		}		
	}

	@Override
	public void onService (Api api, ApiService service, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		SpecAndSpiPair ssp = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (ssp == null) {
			return;
		}

		Object spi = ssp.spi ();
		if (spi == null) {
			return;
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, request);
		if (!engine.has (spi, Functions.OnService)) {
			return;
		}
		
		Object jsApi = ((SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name)).spec ();
		if (jsApi == null) {
			throw new ApiServiceExecutionException ("api or spi not attached on Api OnStart");
		}		

		// invoke onService
		try {
			engine.invoke (spi, Functions.OnService, jsApi, service, request, response);
		} catch (ScriptingEngineException ex) {
			ex.setScript (Json.getString (api.getSpiDef (), Api.Spec.Spi.Function));
			Throwable cause = ex;
			if (ex.getCause () != null) {
				cause = ex.getCause ();
			}
			ApiResponse.Status status = null;
			if (cause instanceof NashornException && cause.getCause () != null) {
				if (cause.getCause () instanceof ApiServiceExecutionException) {
					ApiServiceExecutionException see = (ApiServiceExecutionException)cause.getCause ();
					status = see.status ();
				}
			}
			throw new ApiServiceExecutionException (cause.getMessage (), cause).status (status);
		}		
	}

	@Override
	public void onExecute (Api api, ApiConsumer consumer, ApiService service,
			ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		SpecAndSpiPair ssp = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (ssp == null) {
			return;
		}

		Object spi = ssp.spi ();
		if (spi == null) {
			return;
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, request);
		if (!engine.has (spi, Functions.OnExecute)) {
			return;
		}
		
		Object jsApi = ((SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name)).spec ();
		if (jsApi == null) {
			throw new ApiServiceExecutionException ("api or spi not attached on Api OnStart");
		}		
		
		try {
			
			// update consumer id
			if (consumer.getReference () != null) {
				engine.invoke (consumer.getReference (), ConsumerSet, ApiConsumer.Fields.Id, consumer.get (ApiConsumer.Fields.Id));
			}

			// invoke onExecute
			engine.invoke (spi, Functions.OnExecute, jsApi, consumer, service, request, response);
			
		} catch (ScriptingEngineException ex) {
			ex.setScript (Json.getString (api.getSpiDef (), Api.Spec.Spi.Function));
			Throwable cause = ex;
			if (ex.getCause () != null) {
				cause = ex.getCause ();
			}
			ApiResponse.Status status = null;
			if (cause instanceof NashornException && cause.getCause () != null) {
				if (cause.getCause () instanceof ApiServiceExecutionException) {
					ApiServiceExecutionException see = (ApiServiceExecutionException)cause.getCause ();
					status = see.status ();
				}
			}
			throw new ApiServiceExecutionException (cause.getMessage (), cause).status (status);
		}	
	}

	@Override
	public void afterExecute (Api api, ApiConsumer consumer, ApiService service,
			ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		SpecAndSpiPair ssp = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (ssp == null) {
			return;
		}

		Object spi = ssp.spi ();
		if (spi == null) {
			return;
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, request);
		if (!engine.has (spi, Functions.AfterExecute)) {
			return;
		}
		
		Object jsApi = ((SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name)).spec ();
		if (jsApi == null) {
			throw new ApiServiceExecutionException ("api or spi not attached on Api OnStart");
		}		
		
		try {
			engine.invoke (spi, Functions.AfterExecute, jsApi, consumer, service, request, response);
		} catch (ScriptingEngineException ex) {
			ex.setScript (Json.getString (api.getSpiDef (), Api.Spec.Spi.Function));
			Throwable cause = ex;
			if (ex.getCause () != null) {
				cause = ex.getCause ();
			}
			ApiResponse.Status status = null;
			if (cause instanceof NashornException && cause.getCause () != null) {
				if (cause.getCause () instanceof ApiServiceExecutionException) {
					ApiServiceExecutionException see = (ApiServiceExecutionException)cause.getCause ();
					status = see.status ();
				}
			}
			throw new ApiServiceExecutionException (cause.getMessage (), cause).status (status);
		}	
	}

	@Override
	public void findConsumer (Api api, ApiService service, ApiRequest request, ApiConsumer consumer) throws ApiAuthenticationException {
		SpecAndSpiPair ssp = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (ssp == null) {
			return;
		}

		Object spi = ssp.spi ();
		if (spi == null) {
			return;
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, request);
		if (!engine.has (spi, Functions.FindConsumer)) {
			return;
		}
		
		Object jsApi = ((SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name)).spec ();
		if (jsApi == null) {
			throw new ApiAuthenticationException ("api or spi not attached on Api OnStart");
		}		

		// invoke findConsumer
		try {
			engine.invoke (spi, Functions.FindConsumer, jsApi, service, request, consumer);
		} catch (ScriptingEngineException ex) {
			ex.setScript (Json.getString (api.getSpiDef (), Api.Spec.Spi.Function));
			throw new ApiAuthenticationException (ex.getMessage (), ex);
		}		
	}

	@Override
	public void onError (Api api, ApiService service, ApiConsumer consumer, ApiRequest request, ApiResponse response, JsonObject error) {
		SpecAndSpiPair ssp = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (ssp == null) {
			return;
		}

		Object spi = ssp.spi ();
		if (spi == null) {
			return;
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, request);
		if (!engine.has (spi, Functions.OnError)) {
			return;
		}

		Object jsApi = ((SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name)).spec ();
		if (api == null || jsApi == null) {
			Object msg = error.get (ApiResponse.Error.Message);
			if (msg != null) {
				msg += Lang.ENDLN + "api or spi not attached on Api OnStart";
			} else {
				msg = Lang.ENDLN + "api or spi not attached on Api OnStart";
			}
			
			api.tracer ().log (Level.Error, msg);
			
			error.set (ApiResponse.Error.Message, msg);
			
			return;
		}		

		// invoke onError
		try {
			engine.invoke (spi, Functions.OnError, jsApi, service, consumer, request, response, error);
		} catch (ScriptingEngineException ex) {
			api.tracer ().log (Level.Error, Lang.BLANK, ex);
			error.set (ApiResponse.Error.Message, ex.getMessage ());
		}		
	}

}

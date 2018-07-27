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
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiServiceSpi;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.ScriptContext;
import com.bluenimble.platform.scripting.ScriptingEngine;
import com.bluenimble.platform.scripting.ScriptingEngine.Supported;
import com.bluenimble.platform.scripting.ScriptingEngineException;
import com.bluenimble.platform.server.plugins.scripting.utils.Converters;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Undefined;

@SuppressWarnings("restriction")
public class ScriptableApiServiceSpi implements ApiServiceSpi {

	private static final long serialVersionUID = 3343879562079449723L;
	
	private static final String ClassField 		= "clazz";
	private static final String ProxyField 		= "proxy";
	
	private static final String ApiOutputClass 	= "ApiOutput";
	
	interface Paths {
		String Scripting 	= "scripting";
			String Api 			= "api";
			String Spi 			= "spi";
	}

	interface Functions {
		String OnStart 		= "onStart";
		String OnStop 		= "onStop";
		String Execute 		= "execute";
	}
	
	@Override
	public void onStart (Api api, ApiService service, ApiContext context) throws ApiManagementException {
		SpecAndSpiPair helper = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (helper == null) {
			throw new ApiManagementException ("api '" + api.getNamespace () + "' doesn't support scripting or couldn't start correctly.");
		}		
		
		Object jsApi = helper.spec ();
		if (jsApi == null) {
			throw new ApiManagementException ("api '" + api.getNamespace () + "' doesn't support scripting");
		}		
		
		String _function = Json.getString (service.getSpiDef (), Api.Spec.Spi.Function);
		if (Lang.isNullOrEmpty (_function)) {
			throw new ApiManagementException ("function not found in " + ApiService.Spec.Spi.class.getSimpleName ().toLowerCase ());
		}
		String [] path = Lang.split (_function, Lang.SLASH);
		
		ApiResource rFunction = null;
		try {
			rFunction = api.getResourcesManager ().get (path);
		} catch (ApiResourcesManagerException ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		}
		
		if (rFunction == null) {
			throw new ApiManagementException ("function '" + Lang.join (path, Lang.SLASH) + "' not found");
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, context);
		
		// create the spec
		Object jsService = null;
		try {
			jsService = engine.invoke (null, ApiService.class.getSimpleName (), service);
		} catch (Exception ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		}
		
		if (jsService == null) {
			throw new ApiManagementException ("can't create 'service spec' js object");
		}
		
		// create the spi
		Object spi = null;
		try {
			spi = engine.eval (Supported.Javascript, api, rFunction, ScriptContext.Empty);
		} catch (Exception ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		}
		
		if (spi == null) {
			throw new ApiManagementException ("script returned an undefined object");
		}
		
		service.setHelper (SpecAndSpiPair.Name, new SpecAndSpiPair  (jsService, spi));
		
		if (!engine.has (spi, Functions.OnStart)) {
			return;
		}
		
		// invoke onStart
		try {
			engine.invoke (spi, Functions.OnStart, jsApi, jsService, context);
		} catch (ScriptingEngineException ex) {
			ex.setScript (_function);
			throw new ApiManagementException (ex.getMessage (), ex);
		}		
	}

	@Override
	public void onStop (Api api, ApiService service, ApiContext context) throws ApiManagementException {
		SpecAndSpiPair apiHelper = (SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name);
		if (apiHelper == null) {
			return;
		}
		
		Object jsApi = apiHelper.spec ();
		if (jsApi == null) {
			return;
		}		

		
		SpecAndSpiPair serviceHelper = (SpecAndSpiPair)service.getHelper (SpecAndSpiPair.Name);

		Object spi = serviceHelper.spi ();
		if (spi == null) {
			return;
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, context);
		if (!engine.has (spi, Functions.OnStop)) {
			return;
		}
		
		// invoke onStop
		try {
			engine.invoke (spi, Functions.OnStop, jsApi, serviceHelper.spec (), context);
		} catch (ScriptingEngineException ex) {
			api.tracer ().log (Level.Error, Lang.BLANK, ex);
		}		
	}

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException {
		Object jsApi = ((SpecAndSpiPair)api.getHelper (SpecAndSpiPair.Name)).spec ();
		if (jsApi == null) {
			throw new ApiServiceExecutionException ("api '" + api.getNamespace () + "' doesn't support scripting");
		}		

		SpecAndSpiPair serviceHelper = (SpecAndSpiPair)request.getService ().getHelper (SpecAndSpiPair.Name);

		Object spi = serviceHelper.spi ();
		if (spi == null) {
			throw new ApiServiceExecutionException ("service spi not found");
		}
		
		ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, request);
		if (!engine.has (spi, Functions.Execute)) {
			return null;
		}
		
		// invoke execute
		Object result = null;
		try {
			result = engine.invoke (spi, Functions.Execute, jsApi, consumer, request, response);
		} catch (ScriptingEngineException ex) {
			ex.setScript (Json.getString (request.getService ().getSpiDef (), ApiService.Spec.Spi.Function));
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}		
		
		if (result == null || (result instanceof Undefined)) {
			return null;
		}
		
		if (ApiOutput.class.isAssignableFrom (result.getClass ())) {
			return (ApiOutput)result;
		}
		
		if (ScriptObjectMirror.class.isAssignableFrom (result.getClass ())) {
			ScriptObjectMirror som = (ScriptObjectMirror)result;
			Object clazz = som.get (ClassField);
			if (clazz == null) {
				return new ApiSomOutput (som);
			}
			if (clazz.equals (ApiOutputClass)) {
				return (ApiOutput)som.getMember (ProxyField);
			}
		}
		
		Object converted = Converters.convert (result);
		
		if (converted instanceof JsonArray) {
			converted = new JsonObject ().set (ApiOutput.Defaults.Items, converted);
		}
		
		if (!(converted instanceof JsonObject)) {
			throw new ApiServiceExecutionException ("result should be a valid json object");
		}		

		return new JsonApiOutput ((JsonObject)converted);
	}

}

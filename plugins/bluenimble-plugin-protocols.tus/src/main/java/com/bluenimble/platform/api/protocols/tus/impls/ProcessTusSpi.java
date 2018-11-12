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
package com.bluenimble.platform.api.protocols.tus.impls;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace.Endpoint;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.protocols.tus.RecyclableTusService;
import com.bluenimble.platform.plugins.protocols.tus.TusProtocolPlugin;
import com.bluenimble.platform.plugins.protocols.tus.impl.TusFileUploadService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.utils.TusUtils;
import com.bluenimble.platform.storage.StorageObject;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public class ProcessTusSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = 2532538390276936715L;
	
	interface Spec {
		String Id 				= "id";
	}
	
	enum Events {
		create,
		append,
		done
	}
	
	private static final DefaultExpressionCompiler ExpressionCompiler = 
			new DefaultExpressionCompiler ("<%", "%>").withScripting (true).cacheSize (100);

	interface ResolverPrefix {
		String Consumer = "consumer";
		String Request 	= "request";
		String File 	= "file";
		String Data 	= "data";
	}

	private TusProtocolPlugin plugin;
	
	public ProcessTusSpi (TusProtocolPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject spi = request.getService ().getSpiDef ();
		
		boolean bypassTenantCheck = Json.getBoolean (spi, TusProtocolPlugin.Spec.BypassTenantCheck, false);
		
		String tusKey = Json.getString (spi, TusProtocolPlugin.Spec.Tus);
		if (Lang.isNullOrEmpty (tusKey)) {
			throw new ApiServiceExecutionException (
				TusProtocolPlugin.Spec.Tus + " not found in service " + ApiService.Spec.Spi.class.getSimpleName ().toLowerCase ()
			).status (ApiResponse.NOT_IMPLEMENTED);
		}
		
		RecyclableTusService tsr = (RecyclableTusService)api.space ().getRecyclable (plugin.createKey (tusKey));
		if (tsr == null) {
			throw new ApiServiceExecutionException (
				TusProtocolPlugin.Spec.Tus + " service " + tusKey + " not found in space"
			).status (ApiResponse.NOT_IMPLEMENTED);
		}
		
		String tenant = null;
		
		api.tracer ().log (Level.Info, "tenant key         {0}", tsr.tenantKey ());
		api.tracer ().log (Level.Info, "tenant placeholder {0}", tsr.tenantPlaceholder ());
		
		switch (tsr.tenantPlaceholder ()) {
			case consumer:
				tenant = (String)consumer.get (Lang.split (tsr.tenantKey (), Lang.DOT));
				break;
			case request:
				tenant = (String)request.get (tsr.tenantKey ());
				break;
			case header:
				tenant = (String)request.get (tsr.tenantKey (), Scope.Header);
				break;
			default:
				break;
		}
		
		TusFileUploadService service = tsr.service ();
		UploadStorageService storage = service.getStorageService ();
		api.tracer ().log (Level.Info, "is multi tenant    {0}", storage.isMultiTenant ());
		
		if (storage.isMultiTenant () && Lang.isNullOrEmpty (tenant) && !bypassTenantCheck) {
			throw new ApiServiceExecutionException (
				tsr.tenantKey () + " not found in consumer" 
			).status (ApiResponse.NOT_FOUND);
		}
		
		// if it's download
		if (ApiVerb.GET.equals (request.getVerb ())) {
			UploadInfo info = null;
			ApiOutput output = null;
			try {
				info = storage.getUploadInfo (request.getPath (), tenant);
				StorageObject object = storage.getData (info, tenant);
				output = object.toOutput (null, info.getFileName (), info.getFileMimeType ());
			} catch (Exception e) {
				throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.NOT_FOUND);
			}
			
			if (output == null) {
				throw new ApiServiceExecutionException ("object " + (String)request.get (Spec.Id) + " not found").status (ApiResponse.NOT_FOUND);
			}
			
			// set metadata header
			if (info.hasMetadata ()) {
                response.set (ApiHeaders.Tus.UploadMetadata, info.getEncodedMetadata ());
            }
			
			return output.set (ApiOutput.Defaults.Disposition, ApiOutput.Disposition.Attachment);
		}
		
		// flush default BNB headers
		response.flushHeaders ();
		
		try {
			HttpServletResponse hResponse = TusUtils.toHttpResponse (response);
			UploadInfo info = service.process (
				TusUtils.toHttpRequest (request), hResponse, tenant
			);
			
			ApiOutput output = null;
			
			JsonObject events = Json.getObject (spi, Events.class.getSimpleName ().toLowerCase ());
			
			switch (request.getVerb ()) {
				case POST:
					output = fireEvent (api, consumer, request, tusKey, Json.getObject (events, Events.create.name ()), info);
					break;
	
				case PATCH:
					if (info.isUploadInProgress ()) {
						output = fireEvent (api, consumer, request, tusKey, Json.getObject (events, Events.append.name ()), info);
					} else {
						output = fireEvent (api, consumer, request, tusKey, Json.getObject (events, Events.done.name ()), info);
					}
					// hResponse.addHeader (HttpHeader.UPLOAD_OFFSET, Objects.toString(info.getOffset ()));
					// hResponse.setStatus (HttpServletResponse.SC_NO_CONTENT);
					break;
	
				default:
					break;
			}
			
			hResponse.addHeader (ApiHeaders.ExecutionTime, String.valueOf (System.currentTimeMillis () - request.getTimestamp ().getTime ()));
			
			api.tracer ().log (Level.Info, "fireEvent output " + (output == null ? "Null" : output.data () ));
			
			if (output != null && !Json.isNullOrEmpty (output.data ())) {
				hResponse.addHeader (ApiHeaders.Application, Base64.encodeBase64String (output.data ().toString ().getBytes ()));
			}
			
			response.commit ();
			
		} catch (Exception ex) {
			if (ex instanceof ApiServiceExecutionException) {
				throw (ApiServiceExecutionException)ex;
			}
			throw new ApiServiceExecutionException (ex.getMessage (), ex)
				.status (request.getService ().getVerb ().equals (ApiVerb.HEAD) ? ApiResponse.NOT_FOUND : ApiResponse.INTERNAL_SERVER_ERROR);
		}
	
		return null;
	}

	private ApiOutput fireEvent (Api api, ApiConsumer consumer, ApiRequest pRequest, String tusKey, JsonObject eventSpec, UploadInfo info) 
			throws ApiServiceExecutionException {
		
		if (Json.isNullOrEmpty (eventSpec)) {
			return null;
		}
		
		api.tracer ().log (Level.Info, "fireEvent with spec  {0}", eventSpec);
		
		final Map<String, Object> bindings = new HashMap<String, Object> ();
		bindings.put (ResolverPrefix.Request, pRequest.toJson ());
		bindings.put (ResolverPrefix.Consumer, consumer.toJson ());
		bindings.put (ResolverPrefix.File, info.toJson (true));
		
		Object data = Json.find ((JsonObject)api.space ().getRuntime (TusProtocolPlugin.Spec.Tus), tusKey, ResolverPrefix.Data);
		if (data != null) {
			bindings.put (ResolverPrefix.Data, data);
		}
		
		VariableResolver variableResolver = new VariableResolver () {
			private static final long serialVersionUID = -485939153491337463L;
			@Override
			public Object resolve (String namespace, String... property) {
				if (Lang.isNullOrEmpty (namespace)) {
					return null;
				}
				
				JsonObject target = (JsonObject)bindings.get (namespace);
				if (target == null) {
					return null;
				}
				
				return Json.find (target, property);
			}
			
			@Override
			public Map<String, Object> bindings () {
				return bindings;
			}
		};
		
		final JsonObject spec = (JsonObject)Json.resolve (eventSpec.duplicate (), ExpressionCompiler, variableResolver);
		
		ApiRequest request = api.space ().request (pRequest, consumer, new Endpoint () {
			@Override
			public String space () {
				return Json.getString (spec, ApiRequest.Fields.Space, api.space ().getNamespace ());
			}
			@Override
			public String api () {
				return Json.getString (spec, ApiRequest.Fields.Api, api.getNamespace ());
			}
			@Override
			public String [] resource () {
				String resource = Json.getString (spec, ApiRequest.Fields.Resource);
				if (resource.startsWith (Lang.SLASH)) {
					resource = resource.substring (1);
				}
				if (resource.endsWith (Lang.SLASH)) {
					resource = resource.substring (0, resource.length () - 1);
				}
				if (Lang.isNullOrEmpty (resource)) {
					return null;
				}
				return Lang.split (resource, Lang.SLASH);
			}
			@Override
			public ApiVerb verb () {
				try {
					return ApiVerb.valueOf (
						Json.getString (spec, ApiRequest.Fields.Verb, ApiVerb.POST.name ()).toUpperCase ()
					);
				} catch (Exception ex) {
					return ApiVerb.POST;
				}
			}
		});
		
		// copy parameters
		JsonObject parameters = Json.getObject (spec, ApiRequest.Fields.Data.Parameters);
		if (!Json.isNullOrEmpty (parameters)) {
			Iterator<String> keys = parameters.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				request.set (key, parameters.get (key), Scope.Parameter);
			}
		}
		
		// copy headers
		JsonObject headers = Json.getObject (spec, ApiRequest.Fields.Data.Headers);
		if (!Json.isNullOrEmpty (headers)) {
			Iterator<String> keys = headers.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				request.set (key, headers.get (key), Scope.Header);
			}
		}
		
		return api.call (request);
		
	}

}

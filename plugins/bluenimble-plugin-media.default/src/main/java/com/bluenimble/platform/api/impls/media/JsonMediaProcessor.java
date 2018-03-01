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
package com.bluenimble.platform.api.impls.media;

import java.io.IOException;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Lang.VariableResolver;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiMediaException;
import com.bluenimble.platform.api.ApiMediaProcessor;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngine;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.AbstractEmitter;
import com.bluenimble.platform.json.JsonEmitter;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.plugins.media.MediaPlugin;
import com.bluenimble.platform.server.plugins.media.utils.DefaultVariableResolver;
import com.bluenimble.platform.server.plugins.media.utils.MediaRoutingUtils;
import com.bluenimble.platform.server.plugins.media.utils.WriteResponseUtils;

public class JsonMediaProcessor implements ApiMediaProcessor {

	private static final long serialVersionUID = -3490327410756493328L;
	
	protected MediaPlugin plugin;
	
	public JsonMediaProcessor (MediaPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void process (Api api, ApiService service, ApiConsumer consumer, ApiOutput output, ApiRequest request, final ApiResponse response) 
			throws ApiMediaException {

		String contentType = (String)request.get (ApiRequest.MediaType);
		if (Lang.isNullOrEmpty (contentType)) {
			contentType = ApiContentTypes.Json;
		}

		String 		charset = null;
		JsonObject 	mediaDef = null;
		
		try {
			
			JsonObject mediaSet = service == null ? null : service.getMedia ();
			if (mediaSet != null && !mediaSet.isEmpty ()) {
				mediaDef = Json.getObject (mediaSet, ApiContentTypes.Json);
			}
			if (mediaDef == null) {
				mediaDef = Json.getObject (mediaSet, Lang.STAR);
			}
			
			String rContentType = contentType;
			
			if (output != null) {
				charset = (String)output.get (ApiOutput.Defaults.Charset);
				if (!Lang.isNullOrEmpty (charset)) {
					rContentType += "; charset=" + charset;
				}
			}
			
			VariableResolver vr = new DefaultVariableResolver (request, output != null ? output.data () : null, output != null ? output.meta () : null);
			
			JsonObject media = MediaRoutingUtils.processMedia (
				request, response, 
				vr, 
				mediaDef, api.tracer ()
			);
			
			if (media != null) {
				// if there is a template
				String sTemplate = Json.getString (media, ApiService.Spec.Media.Resource);
				
				ApiResource template = null;
				
				if (!Lang.isNullOrEmpty (sTemplate)) {
					template = api.getResourcesManager ().get (Lang.split (Lang.resolve (sTemplate, vr), Lang.SLASH)); 
				}
				
				if (template != null) {
					
					TemplateEngine engine = plugin.loockupEngine (api, Json.getString (media, ApiService.Spec.Media.Engine, MediaPlugin.HandlebarsEngine));
					
					response.set (ApiHeaders.ContentType, rContentType);
					
					response.flushHeaders ();
					
					engine.write (consumer, request, response, output, template, media);
					
					response.close ();
					return;
				} 
			}
			
			if (WriteResponseUtils.writeError (response, api.tracer (), contentType)) {
				response.close ();
				return;
			}
			
			response.set (ApiHeaders.ContentType, contentType);
			
			if (output == null) {
				response.write (Lang.EMTPY_OBJECT);
				response.close ();
				return;
			} 
			
			JsonObject json = output.data ();
			if (json == null) {
				response.write (Lang.EMTPY_OBJECT);
				response.close ();
				return;
			} 
			
			json.write (new AbstractEmitter () {
				@Override
				public JsonEmitter write (String chunk) {
					try {
						response.write (chunk);
					} catch (IOException e) {
						throw new RuntimeException (e.getMessage (), e);
					}
					return this;
				}
			});
			
			response.close ();

		} catch (Exception e) {
			throw new ApiMediaException (e.getMessage (), e);
		}
		
	}

}

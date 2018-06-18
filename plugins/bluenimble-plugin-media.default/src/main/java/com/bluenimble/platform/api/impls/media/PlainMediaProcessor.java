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

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Lang.VariableResolver;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiMediaException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.media.ApiMediaProcessor;
import com.bluenimble.platform.api.media.DataWriter;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.plugins.media.MediaPlugin;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngine;
import com.bluenimble.platform.api.impls.media.writers.impls.JsonWriter;
import com.bluenimble.platform.api.impls.media.writers.impls.TextWriter;
import com.bluenimble.platform.api.impls.media.writers.impls.YamlWriter;
import com.bluenimble.platform.server.plugins.media.utils.DefaultVariableResolver;
import com.bluenimble.platform.server.plugins.media.utils.MediaRoutingUtils;
import com.bluenimble.platform.server.plugins.media.utils.WriteResponseUtils;

/**
 * 
	"media": {
		"text/html": {
			"success": {
				"*": {
					"resource": "templates/html/links-all.html", 
					"headers": {
						"location": "/myservices"
					}
				},
				"val-1": {
					"resource": "templates/html/links-1.html",  
					"headers": {
						"xLocation": "http://awebsite.com"
					}
				},
				"val-2": {
					"resource": "templates/html/links-2.html",  
					"headers": {
						"xLocation": "http://awebsite.com"
					}
				}
			},
			"error": {
				"*": {
					"resource": "templates/html/links-error.html", 
					"headers": {
						"location": "/error?uuid=${error}"
					}
				}
			}
		}
	}
 * 
 * 
 *
 */
public class PlainMediaProcessor implements ApiMediaProcessor {

	private static final long serialVersionUID = -3490327410756493328L;
	
	public static final String Name = "plain";
	
	private Map<String, DataWriter> writers = new HashMap<String, DataWriter> ();
	
	protected MediaPlugin 	plugin;
	
	public PlainMediaProcessor (MediaPlugin plugin) {
		this.plugin 		= plugin;
		
		addWriter (ApiContentTypes.Text, new TextWriter ());
		addWriter (ApiContentTypes.Json, new JsonWriter ());
		addWriter (ApiContentTypes.Yaml, new YamlWriter ());
	}
	
	@Override
	public void process (Api api, ApiService service, ApiConsumer consumer, ApiOutput output, ApiRequest request, final ApiResponse response) 
			throws ApiMediaException {

		String contentType = (String)request.get (ApiRequest.SelectedMedia);
		if (Lang.isNullOrEmpty (contentType) || ApiMediaProcessor.Any.equals  (contentType)) {
			contentType = Json.getString (api.getMedia (), Api.Spec.Media.Default, ApiContentTypes.Json);
		}
		
		String 		charset 	= Encodings.UTF8;
		JsonObject 	mediaDef 	= MediaRoutingUtils.pickMedia (api, service, contentType);
		
		try {
			String rContentType = contentType;
			
			if (output != null) {
				String oCharset = (String)output.get (ApiOutput.Defaults.Charset);
				if (!Lang.isNullOrEmpty (oCharset)) {
					charset = oCharset;
				}
			}
			
			rContentType += "; charset=" + charset;
			
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
			
			if (WriteResponseUtils.writeError (response, api.tracer (), rContentType)) {
				response.close ();
				return;
			}
			
			response.set (ApiHeaders.ContentType, rContentType);
			
			DataWriter dataWriter = writers.get (contentType);
			if (dataWriter == null) {
				String extendsContentType = Json.getString (mediaDef, ApiService.Spec.Media.Extends, ApiContentTypes.Json);
				dataWriter = writers.get (extendsContentType);
			}

			dataWriter.write (output, response);
			
			response.close ();

		} catch (Exception e) {
			throw new ApiMediaException (e.getMessage (), e);
		}
		
	}

	@Override
	public void addWriter (String name, DataWriter writer) {
		writers.put (name, writer);
	}
	
	@Override
	public void removeWriter (String name) {
		writers.remove (name);
	}

}

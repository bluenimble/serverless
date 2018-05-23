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
package com.bluenimble.platform.api.impls.media.engines.handlebars;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngine;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngineException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.plugins.media.MediaPlugin;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.TemplateSource;

public class HandlebarsTemplateEngine implements TemplateEngine {

	private static final long serialVersionUID = -3029837568501446988L;
	
	private static final String StartDelimitter 		= "startDelimitter";
	private static final String EndDelimitter 			= "endDelimitter";
	
	private static final String DefaultStartDelimitter 	= "[[";
	private static final String DefaultEndDelimitter 	= "]]";
	
	protected Map<String, CachedTemplate> templates = new ConcurrentHashMap<String, CachedTemplate> ();
	
	private Handlebars engine;
	private Api api;
	private JsonObject features;
	
	public HandlebarsTemplateEngine (MediaPlugin plugin, Api api) {
		this.api = api;
		features = Json.getObject (api.getFeatures (), plugin.getNamespace ());
		engine = new Handlebars (new ResourceTemplateLoader (api));
		engine.startDelimiter (Json.getString (features, StartDelimitter, DefaultStartDelimitter));
		engine.endDelimiter (Json.getString (features, EndDelimitter, DefaultEndDelimitter));
		
		engine.registerHelper ("json", new Helper<JsonObject>() {
			public CharSequence apply (JsonObject data, Options options) {
				return new Handlebars.SafeString (data.toString ());
			}
		});
		engine.registerHelper ("equals", new Helper<Object>() {
			public CharSequence apply (Object right, Options options) throws IOException {
				Object left = options.param (0);
				if (right == null && left == null) {
					return options.fn ();
				} else {
					if (right == null) {
						return options.inverse ();
					}
			        return right.equals (left) ? options.fn () : options.inverse ();
				}
			}
		});
	}
	
	@Override
	public void write (ApiConsumer consumer, ApiRequest request,
			ApiResponse response, ApiOutput output, final ApiResource template, final JsonObject mediaSpec)
			throws TemplateEngineException {
		
		final String uuid = template.path ();
		
		CachedTemplate cTemplate = templates.get (uuid);
		
		if (cTemplate == null || cTemplate.timestamp < template.timestamp ().getTime ()) {
			cTemplate = new CachedTemplate ();
			cTemplate.timestamp = template.timestamp ().getTime ();
			
			TemplateSource source = new TemplateSource () {
				@Override
				public long lastModified () {
					return template.timestamp ().getTime ();
				}
				@Override
				public String filename () {
					return uuid;
				}
				@Override
				public String content () throws IOException {
					
					InputStream is = null;
					
					try {
						is = template.toInput ();
						return IOUtils.toString (is);
					} finally {
						IOUtils.closeQuietly (is);
					}
				}
			};
			
			try {
				cTemplate.template = engine.compile (source);
			} catch (IOException e) {
				throw new TemplateEngineException (e.getMessage (), e);
			}
			
			templates.put (uuid, cTemplate);
			
		}
		
		try {
			
			JsonObject vars = new JsonObject ();
			
			vars.set (Json.getString (features, I18n, I18n), api.i18n (request.getLang ()));

			vars.set (Json.getString (features, Request, Request), request.toJson ());
			if (consumer != null) {
				vars.set (Json.getString (features, Consumer, Consumer), consumer.toJson ());
			}
			
			if (output != null) {
				vars.set (Json.getString (features, Output, Output), output.data ());
				vars.set (Json.getString (features, Meta, Meta), output.meta ());
			}
			vars.set (Json.getString (features, Error, Error), response.getError ());
			
			cTemplate.template.apply (vars, response.toWriter ());
			
		} catch (Exception e) {
			throw new TemplateEngineException (e.getMessage (), e);
		}
		
	}
	
	class CachedTemplate {
		Template 	template;
		long		timestamp;
	}

}

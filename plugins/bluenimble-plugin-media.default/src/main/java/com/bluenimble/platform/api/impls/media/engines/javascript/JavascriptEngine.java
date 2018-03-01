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
package com.bluenimble.platform.api.impls.media.engines.javascript;

import java.io.IOException;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngine;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngineException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.AbstractEmitter;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonEmitter;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.MapScriptContext;
import com.bluenimble.platform.scripting.ScriptingEngine;
import com.bluenimble.platform.server.plugins.media.MediaPlugin;

public class JavascriptEngine implements TemplateEngine {

	private static final long serialVersionUID = -3029837568501446988L;
	
	private static final String DoNotWrite 	= "doNotWrite";

	private JsonObject 		features;
	
	private Api api;
	
	public JavascriptEngine (MediaPlugin plugin, Api api) {
		this.api = api;
		features = Json.getObject (api.getFeatures (), plugin.getName ());
	}
	
	@Override
	public void write (ApiConsumer consumer, ApiRequest request,
			final ApiResponse response, ApiOutput output, final ApiResource template, final JsonObject mediaSpec)
			throws TemplateEngineException {
		
		try {
			
			ScriptingEngine engine = api.space ().feature (ScriptingEngine.class, ApiSpace.Features.Default, null);
			
			MapScriptContext vars = new MapScriptContext ();
			
			vars.set (Json.getString (features, I18n, I18n), api.i18n (request.getLang ()));

			vars.set (Json.getString (features, Request, Request), request.toJson ());
			if (consumer != null) {
				vars.set (Json.getString (features, Consumer, Consumer), consumer.toJson ());
			}
			
			JsonObject jOutput = null;
			if (output != null) {
				jOutput = output.data ();
			}
			
			if (output != null) {
				vars.set (Json.getString (features, Output, Output), jOutput);
				vars.set (Json.getString (features, Meta, Meta), output.meta ());
			}
			vars.set (Json.getString (features, Error, Error), response.getError ());			
			
			vars.set (Json.getString (features, Response, Response), response);
			
			JsonObject result;
			
			Object oResult = engine.eval (ScriptingEngine.Supported.Javascript, api, template, vars);
			if (oResult != null) {
				if (oResult instanceof JsonObject) {
					result = (JsonObject)oResult;
				} else if (oResult instanceof JsonArray) {
					result = new JsonObject ();
					result.set (ApiOutput.Defaults.Items, oResult);
				} else {
					result = new JsonObject ();
					result.set (Output, oResult);
				}
			} else {
				result = jOutput;
			}
			
			if (response.getError () != null) {
				result = response.getError ();
			}
			
			boolean doNotWrite = Json.getBoolean (jOutput, DoNotWrite, false);
			if (!doNotWrite) {
				doNotWrite = Json.getBoolean (mediaSpec, DoNotWrite, false);
			}
			
			if (result != null && !doNotWrite) {
				response.flushHeaders ();
				result.write (new AbstractEmitter () {
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
			}
			
		} catch (Exception e) {
			throw new TemplateEngineException (e.getMessage (), e);
		} 
		
	}
	
}

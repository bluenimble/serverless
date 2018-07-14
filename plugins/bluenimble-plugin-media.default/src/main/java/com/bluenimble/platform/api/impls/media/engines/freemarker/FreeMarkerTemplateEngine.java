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

package com.bluenimble.platform.api.impls.media.engines.freemarker;

import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngine;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngineException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.reflect.BeanUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class FreeMarkerTemplateEngine implements TemplateEngine {
	
	private static final long serialVersionUID = -6678642051445096461L;
	
	interface Spec {
		String Objects = "objects";
	}
	
	private static final String InitMethod = "init";

	private JsonObject 			config;
	
	private Configuration 		freeMarker;
	
	private Map<String, Object> objects;
	
	public FreeMarkerTemplateEngine (Plugin plugin, Api api) throws Exception {
		config = (JsonObject)Json.find (api.getFeatures (), plugin.getNamespace (), Templating);
		JsonObject oObjects = Json.getObject (config, Spec.Objects);
		if (!Json.isNullOrEmpty (oObjects)) {
			objects = new HashMap<String, Object>();
			Iterator<String> keys = oObjects.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				Object value = oObjects.get (key);
				if (value instanceof JsonObject) {
					JsonObject def = (JsonObject)value;
					if (def.containsKey (BeanUtils.Clazz)) {
						value = BeanUtils.create (api.getClassLoader (), (JsonObject)value);
						Method init = value.getClass ().getMethod (InitMethod, new Class [] { Api.class });
						init.invoke (value, new Object [] { api });
					}
				}
				objects.put (
					key, 
					value
				);
			}
		}

		// Create your Configuration instance, and specify if up to what FreeMarker
		// version (here 2.3.27) do you want to apply the fixes that are not 100%
		// backward-compatible. See the Configuration JavaDoc for details.
		freeMarker = new Configuration (Configuration.VERSION_2_3_28);
		
		//freeMarker.setObjectWrapper (new DefaultObjectWrapper (Configuration.VERSION_2_3_28));

		// Specify the source where the template files come from. Here I set a
		// plain directory for it, but non-file-system sources are possible too:
		freeMarker.setTemplateLoader (new FreeMarkerTemplateLoader (plugin, api));

		// Set the preferred charset template files are stored in. UTF-8 is
		// a good choice in most applications:
		freeMarker.setDefaultEncoding (Encodings.UTF8);

		// Sets how errors will appear.
		// During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
		freeMarker.setTemplateExceptionHandler (TemplateExceptionHandler.DEBUG_HANDLER);

		// Don't log exceptions inside FreeMarker that it will thrown at you anyway:
		freeMarker.setLogTemplateExceptions (false);

		// Wrap unchecked exceptions thrown during template processing into TemplateException-s.
		freeMarker.setWrapUncheckedExceptions (true);

	}

	@Override
	public void write (ApiResource template, Map<String, Object> model, Writer writer) throws TemplateEngineException {
		try {
			model.putAll (objects);
			Template tpl = freeMarker.getTemplate (template.path ());
			tpl.process (model, writer);
		} catch (Exception e) {
			throw new TemplateEngineException (e.getMessage (), e);
		}
	}

}

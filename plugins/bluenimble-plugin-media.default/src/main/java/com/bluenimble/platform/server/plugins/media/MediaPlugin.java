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
package com.bluenimble.platform.server.plugins.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.impls.media.PlainMediaProcessor;
import com.bluenimble.platform.api.impls.media.StreamMediaProcessor;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngine;
import com.bluenimble.platform.api.impls.media.engines.TemplateEnginesRegistry;
import com.bluenimble.platform.api.impls.media.engines.freemarker.FreeMarkerTemplateEngine;
import com.bluenimble.platform.api.impls.media.engines.handlebars.HandlebarsTemplateEngine;
import com.bluenimble.platform.api.impls.media.engines.javascript.JavascriptEngine;
import com.bluenimble.platform.api.impls.media.engines.jtwig.JtwigTemplateEngine;
import com.bluenimble.platform.api.media.ApiMediaProcessorRegistry;
import com.bluenimble.platform.api.media.MediaTypeUtils;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;

public class MediaPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	public static final String FreeMarkerEngine		= "fm";	
	public static final String JtwigEngine			= "jtwig";	
	public static final String HandlebarsEngine 	= "hb";
	public static final String JavascriptEngine 	= "js";
	
	private TemplateEnginesRegistry enginesRegistry = new TemplateEnginesRegistry ();
	
	private ApiServer server; 
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		this.server = server;
		
		// install mimes
		InputStream mimes = null;
		try {
			mimes = new FileInputStream (new File (home, "mimes"));
			MediaTypeUtils.install (mimes);
		} finally {
			IOUtils.closeQuietly (mimes);
		}
		
		ApiMediaProcessorRegistry registry = server.getMediaProcessorRegistry ();
		if (registry == null) {
			return;
		}
		
		registry.register (PlainMediaProcessor.Name, new PlainMediaProcessor (this), true);
		registry.register (StreamMediaProcessor.Name, new StreamMediaProcessor (), false);
		
	}

	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {
		if (!Api.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		Api api = (Api)target;
		
		if (event.equals (Event.Start)) {
			try {
				enginesRegistry.add (api, FreeMarkerEngine, new FreeMarkerTemplateEngine (this, api));
			} catch (Exception ex) {
				throw new PluginRegistryException (ex.getMessage (), ex);
			}
			enginesRegistry.add (api, HandlebarsEngine, new HandlebarsTemplateEngine (this, api));
			enginesRegistry.add (api, JtwigEngine, new JtwigTemplateEngine (this, api));
			enginesRegistry.add (api, JavascriptEngine, new JavascriptEngine (this, api));
		} else if (event.equals (Event.Uninstall)) {
			enginesRegistry.remove (api, FreeMarkerEngine);
			enginesRegistry.remove (api, HandlebarsEngine);
			enginesRegistry.remove (api, JtwigEngine);
			enginesRegistry.remove (api, JavascriptEngine);
		} 
	}

	public TemplateEngine loockupEngine (Api api, String name) {
		return enginesRegistry.get (api, name);
	}
	
	public ApiServer server () {
		return server;
	} 
	
}

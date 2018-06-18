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

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.server.plugins.media.MediaPlugin;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;

public class ResourceTemplateLoader implements TemplateLoader {

	private static final String Templates 				= "templates";
	private static final String TemplateExtension 		= "templateExtension";
	
	private static final String DotTpl 				= ".tpl";

	private Api 		api;
	private String 		templatesPath;
	private String 		templateExt;
	
	public ResourceTemplateLoader (Plugin plugin, Api api) {
		this.api 			= api;
		JsonObject config 	= (JsonObject)Json.find (api.getFeatures (), plugin.getNamespace (), MediaPlugin.JtwigEngine);
		templatesPath 		= Json.getString (config, Templates, Templates);
		if (templatesPath.startsWith (Lang.SLASH)) {
			templatesPath = templatesPath.substring (1);
		}
		if (templatesPath.endsWith (Lang.SLASH)) {
			templatesPath = templatesPath.substring (0, templatesPath.length () - 1);
		}
		templateExt 		= Json.getString (config, TemplateExtension, DotTpl);
	}
	
	@Override
	public TemplateSource sourceAt (String name) throws IOException {
		
		if (!name.endsWith (templateExt)) {
			name = name + templateExt;
		}
		
		final String fileName = templatesPath + Lang.SLASH + name;

		ApiResource res;
		try {
			res = api.getResourcesManager ().get (Lang.split (fileName, Lang.SLASH));
		} catch (ApiResourcesManagerException e) {
			throw new IOException (e.getMessage (), e);
		}
		
		final ApiResource resource = res;
		
		return new TemplateSource () {
			@Override
			public long lastModified () {
				return resource.timestamp ().getTime ();
			}
			@Override
			public String filename () {
				return fileName;
			}
			@Override
			public String content () throws IOException {
				InputStream is = null;
				try {
					is = resource.toInput ();
					return IOUtils.toString (is);
				} finally {
					IOUtils.closeQuietly (is);
				}
			}
		};
	}

	@Override
	public String getPrefix () {
		return null;
	}

	@Override
	public String getSuffix () {
		return null;
	}

	@Override
	public void setPrefix (String prefix) {
		
	}

	@Override
	public void setSuffix (String suffix) {
		
	}

	@Override
	public String resolve (String name) {
		return null;
	}

}

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

package com.bluenimble.platform.api.impls.media.engines.jtwig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import org.jtwig.resource.loader.ResourceLoader;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.plugins.Plugin;
import com.google.common.base.Optional;

public class JtwigTemplateLoader implements ResourceLoader {

	private Api 		api;
	
	public JtwigTemplateLoader (Plugin plugin, Api api) {
		this.api 			= api;
	}
	
	@Override
	public boolean exists (String name) {
		ApiResource resource;
		try {
			resource = api.getResourcesManager ().get (Lang.split (name, Lang.SLASH));
		} catch (ApiResourcesManagerException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
		return resource != null;
	}

	@Override
	public Optional<Charset> getCharset (String uri) {
		return Optional.of (Charset.forName (Encodings.UTF8));
	}

	@Override
	public InputStream load (String name) {
		ApiResource resource;
		try {
			resource = api.getResourcesManager ().get (Lang.split (name, Lang.SLASH));
		} catch (ApiResourcesManagerException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
		if (resource == null) {
			return null;
		}
		try {
			return resource.toInput ();
		} catch (IOException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}

	@Override
	public Optional<URL> toUrl (String uri) {
		return null;
	}

}

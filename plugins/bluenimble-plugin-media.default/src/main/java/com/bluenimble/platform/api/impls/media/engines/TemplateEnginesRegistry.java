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
package com.bluenimble.platform.api.impls.media.engines;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;

public class TemplateEnginesRegistry {

	private Map<String, TemplateEngine> engines = new ConcurrentHashMap<String, TemplateEngine> ();
	
	public TemplateEngine get (Api api, String name) {
		if (Lang.isNullOrEmpty (name)) {
			return null;
		}
		return engines.get (api.getNamespace () + Lang.DOT + name);
	}
	
	public void add (Api api, String name, TemplateEngine engine) {
		engines.put (api.getNamespace () + Lang.DOT + name, engine);
	}
	
	public void remove (Api api, String name) {
		engines.remove (api.getNamespace () + Lang.DOT + name);
	}
	
}

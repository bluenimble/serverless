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
package com.bluenimble.platform.plugins.impls;

import java.io.IOException;
import java.net.URL;

import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.server.ApiServer;

public class PluginClassLoader extends PackageClassLoader {
	
	private Plugin plugin;
	
	public PluginClassLoader (String name, URL [] urls) {
		super (name, ApiServer.class.getClassLoader (), urls, (ClassLoader [])null);
	}

	public Plugin getPlugin () {
		return plugin;
	}

	public void setPlugin (Plugin plugin) {
		this.plugin = plugin;
	}
	
	public void clear () throws IOException {
		plugin = null;
		super.clear ();
	}

}

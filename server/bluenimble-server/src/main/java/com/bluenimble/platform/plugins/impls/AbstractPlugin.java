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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginOperation;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.server.ApiServer.Event;

public abstract class AbstractPlugin implements Plugin {

	private static final long serialVersionUID = -2281312799410904624L;
	
	private 	String 		name;
	private 	String 		title;
	private 	String 		description;
	private 	String 		version;
	
	protected 	File 		home;
	
	private 	boolean 	async 		= false;
	private 	boolean 	closable 	= true;
	private 	boolean 	isolated	= true;
	private 	boolean 	initOnInstall	
										= false;
	
	protected	JsonObject	vendor;
	
	protected	Tracer		tracer;
	
	protected 	JsonObject 	tracingSpec;
	
	private Map<String, PluginOperation> operations;

	@Override
	public JsonObject getVendor () {
		return vendor;
	}

	@Override
	public File getHome () {
		return home;
	}

	@Override
	public void setHome (File home) {
		this.home = home;
	}

	@Override
	public String getName () {
		return name;
	}
	@Override
	public void setName (String name) {
		this.name = name;
	}
	
	@Override
	public String getDescription () {
		return description;
	}
	@Override
	public void setDescription (String description) {
		this.description = description;
	}
	
	@Override
	public String getTitle () {
		return title;
	}
	
	@Override
	public void setTitle (String title) {
		this.title = title;
	}
	
	@Override
	public String getVersion () {
		return version;
	}

	@Override
	public boolean isAsync () {
		return async;
	}
	
	@Override
	public boolean isClosable () {
		return closable;
	}

	@Override
	public boolean isIsolated () {
		return isolated;
	}

	@Override
	public boolean isInitOnInstall () {
		return initOnInstall;
	}
	
	@Override
	public PluginOperation operation (String name) {
		if (operations == null || operations.isEmpty ()) {
			return null;
		}
		return null;
	}
	
	@Override
	public void register (PluginOperation operation) {
		if (operations == null) {
			operations = new HashMap<String, PluginOperation> ();
		}
		operations.put (operation.name (), operation);
	}

	public void setVendor (JsonObject vendor) {
		this.vendor = vendor;
	}

	public void setAsync (boolean async) {
		this.async = async;
	}

	public void setClosable (boolean closable) {
		this.closable = closable;
	}

	public void setIsolated (boolean isolated) {
		this.isolated = isolated;
	}

	public void setInitOnInstall (boolean initOnInstall) {
		this.initOnInstall = initOnInstall;
	}
	
	public void setVersion (String version) {
		this.version = version;
	}

	@Override
	public void onEvent (Event event, Object target) throws PluginRegistryException {
	}
	
	@Override
	public void setTracer (Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public Tracer tracer () {
		return tracer;
	}

	@Override
	public String getNamespace () {
		return getName ();
	}

	@Override
	public void kill () {
		if (tracer != null) {
			tracer.onShutdown (this);
		}
	}

}

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
package com.bluenimble.platform.server.plugins.scripting;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.scripting.ScriptableApiServiceSpi;
import com.bluenimble.platform.api.impls.scripting.ScriptableApiSpi;
import com.bluenimble.platform.api.scripting.impls.DefaultScriptingEngine;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.scripting.ScriptingEngine;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

@SuppressWarnings("restriction")
public class ScriptingPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	interface Vars {
		String Core 	= "core";
		String Tools 	= "tools";
	}
	interface Registered {
		String ApiSpi 		= "ApiSpi";
		String ServiceSpi 	= "ServiceSpi";
	}
	
	private static final NashornScriptEngineFactory  	Manager 			= new NashornScriptEngineFactory ();
	
	private static final ScriptEngine 					MasterEngine 		= Manager.getScriptEngine (ScriptingPlugin.class.getClassLoader ());
	
	private ScriptingEngine shared;
	
	private String vmArgs;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = ScriptingEngine.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		
		PackageClassLoader pcl = (PackageClassLoader)ScriptingPlugin.class.getClassLoader ();
		
		pcl.registerObject (Registered.ApiSpi, new ScriptableApiSpi ());
		pcl.registerObject (Registered.ServiceSpi, new ScriptableApiServiceSpi ());
		
		File platform = new File (home, "platform");
		
		// load platform
		Reader pReader = null;
		try {
			
			pReader = new FileReader (new File (platform, "Platform.js"));
			
			Bindings bindings = new SimpleBindings ();
			bindings.put (Vars.Core, new File (platform, Vars.Core).getAbsolutePath ());
			bindings.put (Vars.Tools, new File (platform, Vars.Tools).getAbsolutePath ());
			
			shared = new DefaultScriptingEngine (this, (ScriptObjectMirror)MasterEngine.eval (pReader, bindings), server.getMapProvider ());
			
		} finally {
			IOUtils.closeQuietly (pReader);
		}
		
		// add features
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = 2626039344401539390L;
			@Override
			public Class<?> type () {
				return ScriptingEngine.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				return shared;
			}
			@Override
			public String provider () {
				return ScriptingPlugin.this.getName ();
			}
			@Override
			public Plugin implementor () {
				return ScriptingPlugin.this;
			}
		});
		
	}
	
	@Override
	public void onEvent (Event event, Object target) {
	}

	public ScriptEngine create () throws Exception {
		if (Lang.isNullOrEmpty (vmArgs)) {
			return Manager.getScriptEngine ();
		} else {
			return Manager.getScriptEngine (vmArgs);
		}
	}
	
	public String getVmArgs () {
		return vmArgs;
	}

	public void setVmArgs (String vmArgs) {
		this.vmArgs = vmArgs;
	}

}

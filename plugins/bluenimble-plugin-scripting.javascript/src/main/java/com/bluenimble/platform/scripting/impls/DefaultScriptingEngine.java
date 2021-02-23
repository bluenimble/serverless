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
package com.bluenimble.platform.scripting.impls;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Referenceable;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.scripting.ScriptContext;
import com.bluenimble.platform.scripting.Scriptable;
import com.bluenimble.platform.scripting.ScriptingEngine;
import com.bluenimble.platform.scripting.ScriptingEngineException;
import com.bluenimble.platform.server.plugins.scripting.ScriptingPlugin;

//import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

@SuppressWarnings("restriction")
public class DefaultScriptingEngine implements ScriptingEngine {

	private static final long serialVersionUID = -7180272972776761605L;
	
	private static final String ScriptStart = "(function () { ";
	private static final String ScriptEnd 	= " })()";

	interface Namespaces {
		String Context 	= "context";
		String Logger 	= "logger";
	}

	private static final String Libs 	= "Libs";
	
	private static final String JavaClass 
										= "JavaClass";
	
	private static final String Native 	= "var native = function (className) { return JavaClass (className.split ('/').join ('.')).static; };";
	
	private static final String Var		= "var";
	
	private static final Set<String> Denied = new HashSet<String> ();
	static {
		Denied.add ("var quit=function(){};");
		Denied.add ("var exit=function(){};");
		// Denied.add ("var print=function(){};");
		Denied.add ("var echo=function(){};");
		Denied.add ("var readFully=function(){};");
		Denied.add ("var readLine=function(){};");
		Denied.add ("var load=function(){};");
		Denied.add ("var loadWithNewGlobal=function(){};");
		Denied.add ("var $ARG=null;");
		Denied.add ("var $ENV=null;");
		Denied.add ("var $EXEC=null;");
		Denied.add ("var $OPTIONS=null;");
		Denied.add ("var $OUT=null;");
		Denied.add ("var $ERR=null;");
		Denied.add ("var $EXIT=null;");
	}

	private Map<Supported, ScriptEngine> engines = new ConcurrentHashMap<Supported, ScriptEngine> ();
	private Map<String, CachedScript> 	 scripts = new ConcurrentHashMap<String, CachedScript> ();
	
	private ScriptObjectMirror platform;
	private ScriptingPlugin plugin;
	
	public DefaultScriptingEngine (ScriptingPlugin plugin, ScriptObjectMirror platform) throws Exception {
		this.plugin 	= plugin;
		this.platform 	= platform;
		engines.put (Supported.Javascript, plugin.create ());
	}
	
	@Override
	public Object eval (Supported supported, final Api api, ApiResource resource, ScriptContext sContext)
			throws ScriptingEngineException {
		
		if (supported == null) {
			throw new ScriptingEngineException ("Unsupported Scripting Engine ");
		}

		ScriptEngine engine = engines.get (supported);
		if (engine == null) {
			throw new ScriptingEngineException ("Unsupported Scripting Engine " + supported);
		}
		
		// get platform libs
		ScriptObjectMirror 	libs 		= (ScriptObjectMirror)platform.get (Libs);
		String [] 			libsKeys 	= libs.getOwnKeys (true);

		CachedScript cached = scripts.get (resource.owner () + Lang.COLON + resource.path ());
		
		Reader reader = null;
		if (cached == null || cached.timestamp < resource.timestamp ().getTime ()) {
			InputStream rio = null;
			try {
				StringBuilder startScript = new StringBuilder (ScriptStart);
				
				// add native function
				startScript.append (Native);

				// add platform libraries
				for (String lib : libsKeys) {
					startScript.append (Var).append (Lang.SPACE).append (lib).append (Lang.EQUALS)
								.append (Libs).append (Lang.DOT).append (lib).append (Lang.SEMICOLON);
				}
				
				if (plugin.isStrictMode ()) {
					for (String d : Denied) {
						startScript.append (d);
					}
				}
				
				String sStartScript = startScript.toString ();
				startScript.setLength (0);
				
				rio = resource.toInput ();
				
				// format String script = platform.callMember (, arg);
				
				List<InputStream> blocks = new ArrayList<InputStream> ();
				blocks.add (new ByteArrayInputStream (sStartScript.getBytes ()));
				blocks.add (rio);
				blocks.add (new ByteArrayInputStream (ScriptEnd.getBytes ()));
				
				reader = new InputStreamReader (new SequenceInputStream (Collections.enumeration (blocks)));
				cached = new CachedScript ();
				cached.script = ((Compilable)engine).compile (reader);
				cached.timestamp = resource.timestamp ().getTime ();
				
				scripts.put (resource.owner () + Lang.COLON + resource.path (), cached);
			
			} catch (Exception e) {
				throw new ScriptingEngineException (e.getMessage (), e);
			} finally {
				IOUtils.closeQuietly (rio);
			}
		}
		
		if (sContext == null) {
			return null;
		}
		
		Bindings bindings = new SimpleBindings ();
		
		bindings.put (JavaClass, new Function<String, Class<?>> () {
			@Override
			public Class<?> apply (String type) {
				try {
					return api.getClassLoader ().loadClass (type);
				} catch (ClassNotFoundException cnfe) {
					throw new RuntimeException(cnfe);
				}
			}
		});
		
		// add platform libraries
		bindings.put (Libs, libs);
		
		try {
			Iterator<String> vars = sContext.vars ();
			while (vars.hasNext ()) {
				String var = vars.next ();
				bindings.put (var, sContext.var (var));
			}
			return cached.script.eval (bindings);
		} catch (ScriptException e) {
			throw new ScriptingEngineException (e.getMessage (), e);
		} finally {
			bindings.clear ();
		}
		
	}
	
	@Override
	public boolean has (Object scriptable, String function) {
		if (!(scriptable instanceof ScriptObjectMirror)) {
			return false;
		}
		ScriptObjectMirror som = (ScriptObjectMirror)scriptable;
		return som.hasMember (function);
	}

	@Override
	public Object invoke (Object scriptable, String function, Object... args)
			throws ScriptingEngineException {
		
		if (scriptable == null) {
			scriptable = platform;
		}
		if (!(scriptable instanceof ScriptObjectMirror)) {
			throw new ScriptingEngineException ("object is not a valid scriptable object");
		}
		
		ScriptObjectMirror som = (ScriptObjectMirror)scriptable;
		
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				Object arg = args [i];
				if (arg == null || arg instanceof ScriptObjectMirror) {
					continue;
				}
				Class<?> type = arg.getClass ();
				Scriptable ann = type.getAnnotation (Scriptable.class);
				if (ann == null) {
					continue;
				}

				Object jsArg = null;
				
				if (Referenceable.class.isAssignableFrom (arg.getClass ())) {
					jsArg = ((Referenceable)arg).getReference ();
				}
				
				if (jsArg == null) {
					jsArg = platform.callMember (ann.name (), arg);
					
					if (Referenceable.class.isAssignableFrom (arg.getClass ())) {
						((Referenceable)arg).setReference (jsArg);
					}
				}
				
				args [i] = jsArg;
			}
		}
		try {
			return som.callMember (function, args);
		} catch (Throwable err) {
			throw new ScriptingEngineException (err.getMessage (), err);
		}
	}
	
	public void clear () {
		if (scripts != null) {
			scripts.clear ();
		}
		if (engines != null) {
			engines.clear ();
		}
	}

	class CachedScript {
		CompiledScript 	script;
		long			timestamp;
	}

}

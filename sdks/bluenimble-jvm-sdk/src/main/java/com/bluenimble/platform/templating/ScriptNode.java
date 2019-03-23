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
package com.bluenimble.platform.templating;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.bluenimble.platform.templating.impls.converters.JsValueConverter;

public class ScriptNode implements Node {

	private static final long serialVersionUID = 5233545719136265499L;
	
	private static final String JavaClass 	= "JavaClass";
	private static final String Imports = 
		"var native 	= function (className) { return JavaClass (className.split ('/').join ('.')).static; };" + 
		"var Lang 		= native ('com.bluenimble.platform.Lang');" +
		"var Base64 	= native ('com.bluenimble.platform.encoding.Base64');" +
		"var System 	= native ('java.lang.System');" +
		"var File 		= native ('java.io.File');" +
		"var Pattern	= native ('java.util.regex.Pattern');" +
		"var FileUtils 	= native ('com.bluenimble.platform.FileUtils');" +
		"var Json 		= native ('com.bluenimble.platform.Json');";
		
	
	private String text;
	private CompiledScript script;
	
	public ScriptNode (ScriptEngine scriptEngine, String text) throws ScriptException {
		script = ((Compilable)scriptEngine).compile (Imports + text);
		this.text = text;
	}
	
	@Override
	public String token () {
		return text;
	}
	
	public Object eval (VariableResolver vr) throws ScriptException {
		Bindings sBindings = new SimpleBindings ();
		sBindings.put (JavaClass, new Function<String, Class<?>> () {
			@Override
			public Class<?> apply (String type) {
				try {
					return ScriptNode.class.getClassLoader ().loadClass (type);
				} catch (ClassNotFoundException cnfe) {
					throw new RuntimeException(cnfe);
				}
			}
		});

		Map<String, Object> bindings = vr.bindings ();
		
		Iterator<String> vars = bindings.keySet ().iterator ();
		while (vars.hasNext ()) {
			String var = vars.next ();
			sBindings.put (var, bindings.get (var));
		}
		
		return JsValueConverter.convert (script.eval (sBindings));
	}
	
}

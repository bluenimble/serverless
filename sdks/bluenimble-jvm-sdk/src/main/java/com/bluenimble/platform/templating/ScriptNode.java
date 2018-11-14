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

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.bluenimble.platform.templating.impls.converters.JsValueConverter;

public class ScriptNode implements Node {

	private static final long serialVersionUID = 5233545719136265499L;
	
	private String text;
	private CompiledScript script;
	
	public ScriptNode (ScriptEngine scriptEngine, String text) throws ScriptException {
		script = ((Compilable)scriptEngine).compile (text);
		this.text = text;
	}
	
	@Override
	public String token () {
		return text;
	}
	
	public Object eval (VariableResolver vr) throws ScriptException {
		Map<String, Object> bindings = vr.bindings ();
		if (bindings == null || bindings.isEmpty ()) {
			return script.eval ();
		} else {
			Bindings sBindings = new SimpleBindings ();
			Iterator<String> vars = bindings.keySet ().iterator ();
			while (vars.hasNext ()) {
				String var = vars.next ();
				sBindings.put (var, bindings.get (var));
			}
			return JsValueConverter.convert (script.eval (sBindings));
		}
	}
	
}

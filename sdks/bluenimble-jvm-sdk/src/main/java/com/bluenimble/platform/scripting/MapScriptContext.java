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
package com.bluenimble.platform.scripting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.iterators.EmptyIterator;

public class MapScriptContext implements ScriptContext {

	private static final long serialVersionUID = 1177694049922520927L;
	
	public static final EmptyIterator<String> EmptyIterator = new EmptyIterator<String> ();
			
	protected Map<String, Object> vars = new HashMap<String, Object> ();
	
	private boolean readOnly;
	
	public MapScriptContext () {
		this (false);
	}
	
	public MapScriptContext (boolean readOnly) {
		this.readOnly = readOnly;
	}
	
	@Override
	public Iterator<String> vars () {
		if (vars.isEmpty ()) {
			return EmptyIterator;
		}
		return vars.keySet ().iterator ();
	}

	@Override
	public Object var (String name) {
		return vars.get (name);
	}

	public MapScriptContext set (String name, Object var) {
		if (readOnly) {
			return this;
		}
		vars.put (name, var);
		return this;
	}

}

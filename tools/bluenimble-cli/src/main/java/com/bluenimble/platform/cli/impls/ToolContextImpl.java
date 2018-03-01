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
package com.bluenimble.platform.cli.impls;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.cli.ToolContext;

public class ToolContextImpl implements ToolContext {
	
	private static final long serialVersionUID = 5298999852289339438L;
	
	private String name;
	private Map<String, Object> cache = new HashMap<String, Object> ();
	private String delimiter = "\n";

	public ToolContextImpl (String name, String delimiter) {
		this.name = name;
		this.delimiter = delimiter;
		put (VARS, new HashMap<String, Object> ());
		
	}

	public ToolContextImpl (String name) {
		this (name, "\n");
	}

	public String getName() {
		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

	@Override
	public Object get (String name) {
		return cache.get (name);
	}

	@Override
	public Iterator<String> keys() {
		return cache.keySet ().iterator ();
	}

	@Override
	public void put (String name, Object value) {
		cache.put (name, value);
	}
	
	@Override
	public String getDelimiter () {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public Map<String, Object> getCache() {
		return cache;
	}

	public void setCache(Map<String, Object> cache) {
		this.cache = cache;
	}

	@Override
	public void remove(String name) {
		cache.remove (name);
	}
	
	@Override
	public String toString () {
		return "Ctx(" + name + ", " + delimiter + "): " + cache;
	}
	
}

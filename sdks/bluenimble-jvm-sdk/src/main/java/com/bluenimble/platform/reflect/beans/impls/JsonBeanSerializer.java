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
package com.bluenimble.platform.reflect.beans.impls;

import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.BeanSchema;
import com.bluenimble.platform.reflect.beans.BeanSerializer;

public class JsonBeanSerializer implements BeanSerializer {

	private static final long serialVersionUID = -3195816029341762129L;
	
	private static final Set<String> Protected = new HashSet<String> ();
	static {
		Protected.add ("password");
	}
	
	public static final BeanSchema 		AllFieldsBeanSchema = new AllFieldsBeanSchema ();
	public static final BeanSchema 		MinFieldsBeanSchema = new MinimalFieldsBeanSchema ();
	
	protected BeanSchema schema;
	
	public JsonBeanSerializer (JsonObject schema) {
		this.schema = new JsonBeanSchema (schema);
	}
	
	@Override
	public BeanSchema schema () {
		return schema;
	}

	@Override
	public JsonObject create (String type, int level) {
		return new JsonObject ();
	}

	@Override
	public void set (String type, JsonObject json, String key, Object value) {
		if (Protected.contains (key.toLowerCase ())) {
			return;
		}
		json.set (key, value);
	}

	@Override
	public void end (String type, JsonObject json) {
		// ignore
	}

}

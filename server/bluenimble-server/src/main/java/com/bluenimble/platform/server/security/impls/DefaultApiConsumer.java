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
package com.bluenimble.platform.server.security.impls;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable (name = "ApiConsumer")
public class DefaultApiConsumer implements ApiConsumer {
	
	private static final long serialVersionUID = 7974605752893834274L;
	
	private Object 	reference;
	
	private Type 	type;
	
	private JsonObject data = (JsonObject)new JsonObject ().set (ApiConsumer.Fields.Anonymous, true);
	
	public DefaultApiConsumer (Type type) {
		this.type = type;
	}
	
	@Override
	public JsonObject toJson () {
		return data;
	}

	@Override
	public Object get (String property) {
		return data.get (property);
	}

	@Override
	public void set (String property, Object value) {
		data.set (property, value);
	}

	@Override
	public Type type () {
		return type;
	}

	@Override
	public boolean isAnonymous () {
		return Json.getBoolean (data, ApiConsumer.Fields.Anonymous, false);
	}

	@Override
	public Object getReference () {
		return reference;
	}

	@Override
	public void setReference (Object reference) {
		this.reference = reference;
	}

	@Override
	public void override (ApiConsumer consumer) {
		DefaultApiConsumer dConsumer = (DefaultApiConsumer)consumer;
		data 		= dConsumer.data;
		type 		= dConsumer.type;
		reference 	= dConsumer.reference;
	}

}

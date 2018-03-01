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
package com.bluenimble.platform.server.plugins.media.utils;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Lang.VariableResolver;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.json.JsonObject;

public class DefaultVariableResolver implements VariableResolver {

	private interface Namespaces {
		String Request 	= "request";
		String Output 	= "output";
		String Extra 	= "extra";
	}
	
	private ApiRequest request;
	private JsonObject output;
	private JsonObject extra;
	
	public DefaultVariableResolver (ApiRequest request, JsonObject output, JsonObject extra) {
		this.request = request;
		this.output = output;
		this.extra = extra;
	}

	@Override
	public String resolve (String ns, String name) {
		if (Lang.isNullOrEmpty (ns) || Namespaces.Request.equals (ns)) {
			JsonObject rJson = request.toJson ();
			Object v = rJson.find (name, Lang.DOT);
			if (v != null) {
				return String.valueOf (v);
			}
			return (String)request.get (name, Scope.Parameter, Scope.Header);
		} else if (Namespaces.Output.equals (ns) && output != null) {
			Object v = output.find (name, Lang.DOT);
			if (v == null) {
				return null;
			}
			return String.valueOf (v);
		} else if (Namespaces.Extra.equals (ns) && extra != null) {
			Object v = extra.find (name, Lang.DOT);
			if (v == null) {
				return null;
			}
			return String.valueOf (v);
		}
		return null;
	}
	
}

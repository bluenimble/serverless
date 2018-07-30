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
package com.bluenimble.platform.server.visitors.impls.actions;

import java.util.Iterator;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.impls.SelectiveApiRequestVisitor.Placeholder;
import com.bluenimble.platform.json.JsonObject;

public class ReplaceAction implements RewriteAction {

	private static final long serialVersionUID = -2165577306745099563L;

	public String [] apply (ApiRequest request, Placeholder placeholder, String [] aTarget, Object value, String conditionValue) {
		
		String sTarget = placeholder.equals (Placeholder.path) ? request.getPath () : request.getEndpoint ();
		
		if (value == null) {
			return aTarget;
		}

		if (value instanceof String) {
			String sValue = (String)value;
			if (conditionValue != null) {
				sTarget = Lang.replace (sTarget, conditionValue, sValue);
				sTarget = trim (sTarget, placeholder);
				return Lang.split (sTarget, placeholder.equals (Placeholder.path) ? Lang.SLASH : Lang.DOT);
			}
		} else if (value instanceof JsonObject) {
			JsonObject tokens = (JsonObject)value;
			if (!Json.isNullOrEmpty (tokens)) {
				Iterator<String> keys = tokens.keys ();
				while (keys.hasNext ()) {
					String key = keys.next ();
					sTarget = Lang.replace (sTarget, key, Json.getString (tokens, key));
				}
				sTarget = trim (sTarget, placeholder);
				return Lang.split (sTarget, placeholder.equals (Placeholder.path) ? Lang.SLASH : Lang.DOT);
			}
		}
		
		return aTarget;
		
	}
	
	private String trim (String sTarget, Placeholder placeholder) {
		if (placeholder.equals (Placeholder.endpoint)) {
			return sTarget;
		}
		if (sTarget.startsWith (Lang.SLASH)) {
			sTarget = sTarget.substring (1);
		}

		if (sTarget.endsWith (Lang.SLASH)) {
			sTarget = sTarget.substring (0, sTarget.length () - 1);
		}
		return sTarget;
	}
	
}

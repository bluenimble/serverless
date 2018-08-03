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
package com.bluenimble.platform.templating.impls.converters;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.json.JsonParser;
import com.bluenimble.platform.templating.ValueConverter;

public class JsonValueConverter implements ValueConverter {

	@Override
	public Object convert (Object value, String spec) {
		if (value instanceof JsonObject || value instanceof JsonArray) {
			return value;
		}
		
		String sValue = String.valueOf (value);
		
		if (Lang.isNullOrEmpty (sValue)) {
			return null;
		}
		try {
			return JsonParser.parse (sValue);
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	}

}

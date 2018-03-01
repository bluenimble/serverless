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
package com.bluenimble.platform.server.plugins.scripting.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.api.ApiResponse;

public class HttpStatuses {

	public static Map<Integer, ApiResponse.Status> Map = new HashMap<Integer, ApiResponse.Status> ();
	static {
		try {
			Field [] fields = ApiResponse.class.getDeclaredFields ();
			for (Field f : fields) {
				if (f.getType ().equals (ApiResponse.Status.class)) {
					ApiResponse.Status s = (ApiResponse.Status)f.get (null);
					Map.put (s.getCode (), s);
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	}
	
}

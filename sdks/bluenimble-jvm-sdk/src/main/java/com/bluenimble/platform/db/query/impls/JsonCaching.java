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
package com.bluenimble.platform.db.query.impls;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.db.query.Caching;
import com.bluenimble.platform.json.JsonObject;

public class JsonCaching implements Caching {

	private static final long serialVersionUID = -4482690420390364506L;
	
	protected JsonObject source;
	
	public JsonCaching (JsonObject source) {
		this.source = source;
	}

	@Override
	public boolean cache (Target target) {
		if (target == null) {
			return false;
		}
		switch (target) {
			case meta:
				return Json.getBoolean (source, target.name (), true);
			case data:
				return Json.getBoolean (source, target.name (), false);
			default:
				break;
		}
		return false;
	}

}

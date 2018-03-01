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
package com.bluenimble.platform.db.impls;

import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.db.DatabaseObjectSerializer;
import com.bluenimble.platform.json.JsonObject;

public class DefaultDatabaseObjectSerializer implements DatabaseObjectSerializer {

	private static final long serialVersionUID = -3195816029341762129L;
	
	private static final Set<String> Protected = new HashSet<String> ();
	static {
		Protected.add ("password");
	}
	
	public static final DatabaseObjectSerializer Instance = new DefaultDatabaseObjectSerializer (0, 1);
	
	protected int 		allStopLevel = 0;
	protected int 		minStopLevel = 1;
	
	public DefaultDatabaseObjectSerializer (int allStopLevel, int minStopLevel) {
		if (allStopLevel < 0) {
			allStopLevel = 0;
		}
		if (minStopLevel < 0) {
			minStopLevel = 0;
		}
		if (minStopLevel < allStopLevel) {
			minStopLevel = allStopLevel;
		}
		this.allStopLevel 	= allStopLevel;
		this.minStopLevel 	= minStopLevel;
	}
	
	@Override
	public Fields fields (int level) {
		if (level <= allStopLevel) {
			return Fields.All;
		} else if (level <= minStopLevel) {
			return Fields.Min;
		} else {
			return Fields.None;
		}
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

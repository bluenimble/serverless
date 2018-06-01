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

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.cache.Cache;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.indexer.Indexer;
import com.bluenimble.platform.messaging.Messenger;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.storage.Storage;

public class FeaturesUtils {
	
	public interface Features {
		String Database 	= "database";
		String DataSource 	= "datasource";
		String Storage 		= "storage";
		String Messenger 	= "messenger";
		String Cache 		= "cache";
		String Remote 		= "remote";
		String Indexer 		= "indexer";
	}
	
	private static final Map<String, Class<?>> FeaturesMap = new HashMap<String, Class<?>> ();
	static {
		FeaturesMap.put (Features.Database, Database.class);
		FeaturesMap.put (Features.Storage, Storage.class);
		FeaturesMap.put (Features.Messenger, Messenger.class);
		FeaturesMap.put (Features.Cache, Cache.class);
		FeaturesMap.put (Features.Remote, Remote.class);
		FeaturesMap.put (Features.Indexer, Indexer.class);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T feature (ApiSpace space, String featureName, ApiContext context, String name) {
		if (name == null) {
			name = ApiSpace.Features.Default;
		}
		Class<T> feature = (Class<T>)FeaturesMap.get (featureName);
		return space.feature (feature, name, context);
	}

}

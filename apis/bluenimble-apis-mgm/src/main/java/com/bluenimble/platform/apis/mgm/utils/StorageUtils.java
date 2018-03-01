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
package com.bluenimble.platform.apis.mgm.utils;

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.impls.filters.ContainsFilter;
import com.bluenimble.platform.storage.impls.filters.EmptyFoldersFilter;
import com.bluenimble.platform.storage.impls.filters.EndsWithFilter;
import com.bluenimble.platform.storage.impls.filters.ExpressionFilter;
import com.bluenimble.platform.storage.impls.filters.NotEmptyFoldersFilter;
import com.bluenimble.platform.storage.impls.filters.OnlyFoldersFilter;
import com.bluenimble.platform.storage.impls.filters.StartsWithFilter;

public class StorageUtils {

	
	private static final Map<String, Folder.Filter> SingletonFilters = new HashMap<String, Folder.Filter> ();
	static {
		SingletonFilters.put ("folders", new OnlyFoldersFilter ());
		SingletonFilters.put ("files", new OnlyFoldersFilter ());
		SingletonFilters.put ("empty", new EmptyFoldersFilter ());
		SingletonFilters.put ("notempty", new NotEmptyFoldersFilter ());
	}
	
	private static final Map<String, Class<? extends Folder.Filter>> DynamicFilters = new HashMap<String, Class<? extends Folder.Filter>> ();
	static {
		DynamicFilters.put ("start", StartsWithFilter.class);
		DynamicFilters.put ("end", EndsWithFilter.class);
		DynamicFilters.put ("contain", ContainsFilter.class);
		DynamicFilters.put ("exp", ExpressionFilter.class);
	}
	
	public static Folder.Filter guessFilter (String filter) throws Exception {
		
		if (Lang.isNullOrEmpty (filter) || filter.equals (Lang.STAR)) {
			return null;
		}
		
		String lFilter = filter.toLowerCase ();
		
		Folder.Filter oFilter = SingletonFilters.get (lFilter);
		if (oFilter != null) {
			return oFilter;
		}
		
		int indexOfColon = filter.indexOf (Lang.COLON);
		if (indexOfColon < 0) {
			return null;
		}
		
		String filterName = filter.substring (0, indexOfColon);
		
		Class<? extends Folder.Filter> cFilter = DynamicFilters.get (filterName);
		if (cFilter == null) {
			return null;
		}
		
		return cFilter.getConstructor (new Class [] { String.class }).newInstance (new Object [] { filter.substring (indexOfColon + 1)} );
		
	}
	
}

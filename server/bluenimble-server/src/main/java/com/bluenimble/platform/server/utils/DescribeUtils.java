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
package com.bluenimble.platform.server.utils;

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.api.DescribeOption;
import com.bluenimble.platform.api.DescribeOption.Option;

public class DescribeUtils {

	public static Map<DescribeOption.Option, DescribeOption> toMap (DescribeOption... options) {
		if (options == null || options.length == 0) {
			return null;
		}
		
		if (options.length == 1 && options [0].getOption ().equals (Option.all)) {
			if (options [0].isVerbose ()) {
				return DescribeOption.AllVerboseOptions;
			}
			return DescribeOption.AllOptions;
		}
		
		Map<DescribeOption.Option, DescribeOption> mOptions = new HashMap<DescribeOption.Option, DescribeOption> ();
		
		for (DescribeOption opt : options) {
			mOptions.put (opt.getOption (), opt);
		}
		
		return mOptions;
	}
	
}

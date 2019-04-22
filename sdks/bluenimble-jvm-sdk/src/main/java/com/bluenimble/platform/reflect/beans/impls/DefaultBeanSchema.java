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
package com.bluenimble.platform.reflect.beans.impls;

import java.util.Set;

import com.bluenimble.platform.reflect.beans.BeanSchema;

public class DefaultBeanSchema implements BeanSchema {
	
	protected int 		allStopLevel = 0;
	protected int 		minStopLevel = 1;
	
	public DefaultBeanSchema (int allStopLevel, int minStopLevel) {
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
	public BeanSchema schema (int level, String field) {
		if (level <= allStopLevel) {
			return BeanSchema.All;
		} else if (level <= minStopLevel) {
			return BeanSchema.Minimal;
		}
		return null;
	}

	@Override
	public Set<String> fields (Set<String> available) {
		return available;
	}

	@Override
	public FetchStrategy fetchStrategy () {
		return FetchStrategy.all;
	}

}

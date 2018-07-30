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
package com.bluenimble.platform.api.impls;

import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.api.ApiRequestVisitor;

public class CompositeApiRequestVisitor extends AbstractApiRequestVisitor {

	private static final long serialVersionUID = 1782406079539122227L;
	
	private List<ApiRequestVisitor> visitors = new ArrayList<ApiRequestVisitor>();

	@Override
	public void visit (AbstractApiRequest request) {
		if (visitors == null || visitors.isEmpty ()) {
			return;
		}
		for (ApiRequestVisitor visitor : visitors) {
			visitor.visit (request);
		}
	}
	
	public void addVisitor (ApiRequestVisitor visitor) {
		visitors.add (visitor);
	}

}

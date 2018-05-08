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
package com.bluenimble.platform.server.impls;

import is.tagomor.woothee.Classifier;

import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.impls.AbstractApiRequest;
import com.bluenimble.platform.json.JsonObject;

public class ExtendedApiRequestVisitor extends DefaultApiRequestVisitor {

	private static final long serialVersionUID = 1782406079539122227L;
	
	public ExtendedApiRequestVisitor (JsonObject spec) {
		setSpec (spec);
	}
	
	@Override
	protected void enrich (AbstractApiRequest request) {
		JsonObject device = request.getDevice ();
		
		String agent = (String)request.get (ApiHeaders.UserAgent, Scope.Header);
		if (agent != null) {
			device.set (ApiRequest.Fields.Device.Agent, agent);
			device.putAll (Classifier.parse (agent));
		}
		
	}
	

}

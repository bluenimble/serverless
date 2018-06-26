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
package com.bluenimble.platform.server.plugins.security;

import com.bluenimble.platform.api.security.impls.BasicConsumerResolver;
import com.bluenimble.platform.api.security.impls.CookieConsumerResolver;
import com.bluenimble.platform.api.security.impls.KeyConsumerResolver;
import com.bluenimble.platform.api.security.impls.SignatureConsumerResolver;
import com.bluenimble.platform.api.security.impls.TokenConsumerResolver;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;

public class ApiSecurityPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;

	@Override
	public void init (final ApiServer server) throws Exception {
		server.addConsumerResolver (new TokenConsumerResolver ());
		server.addConsumerResolver (new CookieConsumerResolver ());
		server.addConsumerResolver (new SignatureConsumerResolver ());
		server.addConsumerResolver (new KeyConsumerResolver ());
		server.addConsumerResolver (new BasicConsumerResolver ());
	}
	
}

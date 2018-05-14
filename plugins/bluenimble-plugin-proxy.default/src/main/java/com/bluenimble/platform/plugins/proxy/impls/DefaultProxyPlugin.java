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
package com.bluenimble.platform.plugins.proxy.impls;

import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.api.impls.proxy.RemoteApiServiceSpi;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;

public class DefaultProxyPlugin extends AbstractPlugin {

	private static final long serialVersionUID = -7715328225346939289L;
	
	interface Registered {
		String ApiSpi 		= "ApiSpi";
		String ServiceSpi 	= "ServiceSpi";
	}

	@Override
	public void init (ApiServer server) throws Exception {
		PackageClassLoader pcl = (PackageClassLoader)DefaultProxyPlugin.class.getClassLoader ();
		pcl.registerObject (Registered.ServiceSpi, new RemoteApiServiceSpi ());
	}

}

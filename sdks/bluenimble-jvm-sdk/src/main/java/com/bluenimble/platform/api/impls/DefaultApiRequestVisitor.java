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

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequestVisitor;
import com.bluenimble.platform.json.JsonObject;

public class DefaultApiRequestVisitor implements ApiRequestVisitor {

	private static final long serialVersionUID = 1782406079539122227L;
	
	private static final String Root = "/sys/mgm/instance/all";
	
	@Override
	public void visit (AbstractApiRequest request) {
		
		// set device
		JsonObject device = request.getDevice ();
		if (device == null) {
			device = new JsonObject ();
		}
		request.setDevice (device);
		
		// set space, api and resource
		String path 	= request.getPath ();
		
		if (Lang.SLASH.equals (path)) {
			path = Root;
		}
		
		if (path != null && path.startsWith (Lang.SLASH)) {
			path = path.substring (1);
		}
		if (path != null && path.endsWith (Lang.SLASH)) {
			path = path.substring (0, path.length () - 1);
		}
		if (Lang.isNullOrEmpty (path)) {
			path = null;
		}
		
		// rewrite
		String [] aPath 	= Lang.split (path, Lang.SLASH);
		
		if (aPath == null || aPath.length == 0) {
			return;
		}
		
		request.setSpace (aPath [0]);
		
		if (aPath.length > 1) {
			request.setApi (aPath [1]);
		}
		if (aPath.length > 2) {
			request.setResource (Lang.moveLeft (aPath, 2));
		}
		
	}

}

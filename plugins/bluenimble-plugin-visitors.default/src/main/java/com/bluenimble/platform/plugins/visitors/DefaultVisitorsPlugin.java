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
package com.bluenimble.platform.plugins.visitors;

import java.lang.reflect.Method;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequestVisitor;
import com.bluenimble.platform.api.impls.CompositeApiRequestVisitor;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.reflect.BeanUtils;
import com.bluenimble.platform.server.ApiServer;

public class DefaultVisitorsPlugin extends AbstractPlugin {

	private static final long serialVersionUID = -7715328225346939289L;
	
	private JsonObject spec;
	private JsonObject visitors;
	
	interface Spec {
		String Visitor = "visitor";
	}

	@Override
	public void init (ApiServer server) throws Exception {
		spec.shrink ();
		
		Object oVisitorId = Json.getString (spec, Spec.Visitor);
		if (oVisitorId == null) {
			return;
		}
		
		ApiRequestVisitor visitor = null;
		
		// if composite
		if (oVisitorId instanceof JsonArray) {
			CompositeApiRequestVisitor cVisitor = new CompositeApiRequestVisitor ();
			
			JsonArray aVisitors = (JsonArray)oVisitorId;
			for (Object v : aVisitors) {
				cVisitor.addVisitor (create (server, String.valueOf (v)));
			}
			visitor = cVisitor;
		} else if (oVisitorId instanceof String) {
			visitor = create (server, String.valueOf (oVisitorId));
		}
		
		server.tracer ().log (Level.Info, "Request Visitor: {0}", visitor);
		
		if (visitor != null) {
			server.setRequestVisitor (visitor);
		}
	}
	
	private ApiRequestVisitor create (ApiServer server, String visitorId) throws Exception {
		if (Lang.isNullOrEmpty (visitorId)) {
			return null;
		}

		JsonObject oVisitor = Json.getObject (visitors, visitorId);
		if (Json.isNullOrEmpty (oVisitor)) {
			return null;
		}
		
		ApiRequestVisitor visitor = (ApiRequestVisitor)BeanUtils.create (ApiServer.class.getClassLoader (), oVisitor, server.getPluginsRegistry ());
		
		Class<? extends ApiRequestVisitor> cVisitor = visitor.getClass ();

		// set spec
		Method setSpec = null;
		try {
			setSpec = cVisitor.getMethod ("setSpec", new Class [] { JsonObject.class } );
		} catch (Exception ex) {
			// IGNOR
		}
		
		if (setSpec != null) {
			setSpec.invoke (visitor, new Object [] { spec } );
		}
		
		// set server
		Method setServer = null;
		try {
			setServer = cVisitor.getMethod ("setServer", new Class [] { ApiServer.class } );
		} catch (Exception ex) {
			// IGNOR
		}
		
		if (setServer != null) {
			setServer.invoke (visitor, new Object [] { server } );
		}
		
		return visitor;
		
	}

	public JsonObject getVisitors () {
		return visitors;
	}

	public void setVisitors (JsonObject visitors) {
		this.visitors = visitors;
	}

	public JsonObject getSpec () {
		return spec;
	}

	public void setSpec (JsonObject spec) {
		this.spec = spec;
	}

}

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
package com.bluenimble.platform.apis.mgm;

import java.io.InputStream;
import java.util.Iterator;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.impls.spis.AbstractApiSpi;
import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.security.ApiConsumer.Type;
import com.bluenimble.platform.json.JsonObject;

public class MgmApiSpi extends AbstractApiSpi {

	private static final long serialVersionUID = 8197725424778011778L;

	private static final String [] Consumers = new String [] {"consumers.json"};
	
	private JsonObject consumers;
	
	@Override
	public void onStart (Api api, ApiContext context) {
		ApiResource resource;
		try {
			resource = api.getResourcesManager ().get (Consumers);
		} catch (ApiResourcesManagerException ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
		if (resource == null) {
			return;
		}
		InputStream stream = null;
		try {
			stream = resource.toInput ();
			consumers = Json.load (stream);
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (stream);
		}
	}

	@Override
	public void findConsumer (Api api, ApiService service, ApiRequest request, ApiConsumer consumer) throws ApiAuthenticationException {
		Type type 	= consumer.type ();
		
		if (ApiRequest.Channels.container.name ().equals (request.getChannel ())) {
			consumer.override (
				(ApiConsumer)request.get (ApiRequest.Consumer)
			);
            return;
        }
		
        if (!this.isSecure (service) ) {
			return;
		}
		
		if (!type.equals (Type.Signature)) {
			throw new ApiAuthenticationException ("unsupported authentication mechanism");
		}
		
		String accessKey	= (String)consumer.get (ApiConsumer.Fields.AccessKey);
		
		final JsonObject oConsumer = Json.getObject (consumers, accessKey);
		if (oConsumer == null || oConsumer.isEmpty ()) {
			throw new ApiAuthenticationException ("accessKey not found");
		}
		
		Iterator<String> keys = oConsumer.keys ();
		if (keys == null) {
			return;
		}
		
		while (keys.hasNext ()) {
			String key = keys.next ();
			consumer.set (key, oConsumer.get (key));
		}
	}
	
	@Override
	public void onStop (Api api, ApiContext context) {
		if (consumers == null) {
			return;
		}
		consumers.clear ();
	}

}

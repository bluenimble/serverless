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

import java.util.Iterator;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiAccessDeniedException;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.spis.AbstractApiSpi;
import com.bluenimble.platform.api.security.ApiAuthenticationException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.security.ApiConsumer.Type;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.security.SpaceKeyStoreException;

public class KeyStoreAwareApiSpi extends AbstractApiSpi {

	private static final long serialVersionUID = 8197725424778011778L;
	
	private KeyPair 	root;
	
	@Override
	public void onStart (Api api, ApiContext context) throws ApiManagementException {
		// get license
		try {
			root = api.space ().getRootKeys ();
		} catch (ApiAccessDeniedException ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		}
	}

	@Override
	public void findConsumer (Api api, ApiService service, ApiRequest request, ApiConsumer consumer) throws ApiAuthenticationException {
		
		if ("container".equals (request.getChannel ())) {
			consumer.override (
				(ApiConsumer)request.get (ApiRequest.Consumer)
			);
            return;
        }
		
        if (!this.isSecure (service) ) {
			return;
		}
		
		if (!consumer.type ().equals (Type.Signature)) {
			throw new ApiAuthenticationException ("unsupported authentication scheme");
		}
		
		JsonArray roles = Json.getArray (service.getSecurity (), ApiService.Spec.Security.Roles);

		String accessKey	= (String)consumer.get (ApiConsumer.Fields.AccessKey);
		
		if (root.accessKey ().equals (accessKey)) {
			if (roles == null || roles.isEmpty () || !roles.contains (Role.SUPER.name ().toLowerCase ())) {
				throw new ApiAuthenticationException ("insuffisant permissions");
			}
			consumer.set (ApiConsumer.Fields.SecretKey, root.secretKey ());
			consumer.set (ApiConsumer.Fields.ExpiryDate, root.expiryDate ());
		} else {
			
			int indexOfDot = accessKey.indexOf (Lang.DOT);
			if (indexOfDot <= 0) {
				throw new ApiAuthenticationException ("invalid accessKey");
			}
			
			String consumerSpaceNs = accessKey.substring (0, indexOfDot);
			
			accessKey = accessKey.substring (indexOfDot + 1);
			
			ApiSpace consumerSpace;
			try {
				consumerSpace = api.space ().space (consumerSpaceNs);
			} catch (ApiAccessDeniedException e) {
				throw new ApiAuthenticationException ("instance manager can't access requested space");
			}
			
			KeyPair skp;
			try {
				skp = consumerSpace.keystore ().get (accessKey, true);
			} catch (SpaceKeyStoreException e) {
				throw new ApiAuthenticationException ("instance manager can't access space keystore");
			}
					
			if (skp == null) {
				throw new ApiAuthenticationException ("accessKey " + accessKey + " not found");
			}
			
			String role = (String)skp.property (CommonSpec.Role);
			
			if (Lang.isNullOrEmpty (role)) {
				throw new ApiAuthenticationException ("no role defined for consumer");
			}
			
			if (roles != null && !roles.isEmpty () && !roles.contains (role.toLowerCase ())) {
				throw new ApiAuthenticationException ("insuffisant permissions");
			}
			
			consumer.set (ApiConsumer.Fields.Space, consumerSpaceNs);
			consumer.set (ApiConsumer.Fields.SecretKey, skp.secretKey ());
			consumer.set (ApiConsumer.Fields.ExpiryDate, skp.expiryDate ());
			
			Iterator<String> props = skp.properties ();
			if (props != null) {
				while (props.hasNext ()) {
					String p = props.next ();
					consumer.set (p, skp.property (p));
				}
			}
		}
		
	}
	
	private boolean isSecure (ApiService service) {
		JsonObject security = service.getSecurity ();
        return 	security == null || 
        		!security.containsKey (ApiService.Spec.Security.Enabled) || 
        		security.get (ApiService.Spec.Security.Enabled) == "true";
	}

}

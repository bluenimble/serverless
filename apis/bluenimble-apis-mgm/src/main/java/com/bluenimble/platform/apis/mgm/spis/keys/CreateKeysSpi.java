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
package com.bluenimble.platform.apis.mgm.spis.keys;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiAccessDeniedException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.apis.mgm.CommonSpec;
import com.bluenimble.platform.apis.mgm.Role;
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.security.SpaceKeyStoreException;

public class CreateKeysSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	interface Spec {
		String Space = "space";
	}
	
	private static final Set<String> Exclude = new HashSet<String> ();
	static {
		Exclude.add (Spec.Space); Exclude.add (KeyPair.Fields.ExpiryDate); Exclude.add (CommonSpec.Role);
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		JsonObject payload = (JsonObject)request.get (ApiRequest.Payload);
		
		Role cRole = Role.valueOf ((String)consumer.get (CommonSpec.Role));
		
		Role role = Role.SUPER.equals (cRole) ? Role.ADMIN : Role.DEVELOPER;
		
		String sRole = Json.getString (payload, CommonSpec.Role);
		if (!Lang.isNullOrEmpty (sRole)) {
			try {
				role = Role.valueOf (sRole.trim ().toUpperCase ());
			} catch (Exception ex) {
				// undefined role
			}
		}
		
		if (Role.SUPER.equals (cRole) && role.equals (Role.DEVELOPER)) {
			throw new ApiServiceExecutionException ("super users can't create developer keys").status (ApiResponse.FORBIDDEN);
		}
		
		if (Role.ADMIN.equals (cRole) && role.equals (Role.ADMIN)) {
			throw new ApiServiceExecutionException ("admin users can't create admin keys").status (ApiResponse.FORBIDDEN);
		}
		
		ApiSpace space;

		if (Role.SUPER.equals (cRole)) {
			String spaceNs = Json.getString (payload, Spec.Space);
			if (Lang.isNullOrEmpty (spaceNs)) {
				throw new ApiServiceExecutionException ("no space found in payload").status (ApiResponse.BAD_REQUEST);
			}
			try {
				space = api.space ().space (spaceNs);
			} catch (ApiAccessDeniedException e) {
				throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.FORBIDDEN);
			}
		} else {
			try {
				space = MgmUtils.space (consumer, api);
			} catch (ApiAccessDeniedException e) {
				throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.FORBIDDEN);
			}
		}
		
		if (space == null) {
			throw new ApiServiceExecutionException ("target space where to create the keys isn't found").status (ApiResponse.BAD_REQUEST);
		} 
		
		Map<String, Object> properties = new HashMap<String, Object> ();
		properties.put (CommonSpec.Role, role.name ());
		
		Date expiryDate = null;
		if (!Json.isNullOrEmpty (payload)) {
			expiryDate = (Date)payload.get (KeyPair.Fields.ExpiryDate);

			Iterator<String> props = payload.keys ();
			while (props.hasNext ()) {
				String p = props.next ();
				if (Exclude.contains (p)) {
					continue;
				}
				properties.put (p, payload.get (p));
			}
		}

		List<KeyPair> list = null;
		try {
			list = space.keystore ().create (1, expiryDate, properties);
		} catch (SpaceKeyStoreException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.BAD_REQUEST);
		}
		
		if (list == null) {
			return new JsonApiOutput (null);
		}

		return new JsonApiOutput (list.get (0).toJson ());
	}

}

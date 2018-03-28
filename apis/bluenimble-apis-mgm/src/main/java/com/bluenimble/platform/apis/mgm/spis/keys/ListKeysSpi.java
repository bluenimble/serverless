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

import java.util.Collection;
import java.util.List;

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
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.security.SpaceKeyStore;

public class ListKeysSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	interface Spec {
		String Offset 	= "offset";
		String Length 	= "length";
		String Filters	= "filters";
	}
	
	interface Output {
		String Keys = "keys";
	}
	
	private static final String Token = "__";
	
	interface Operators {
		String Equals 			= Token + "eq" + Token;
		String NotEquals 		= Token + "neq" + Token;
		String Like 			= Token + "like" + Token;
		String Expired 			= Token + "exp" + Token;
		String NotExpired 		= Token + "nexp" + Token;
		String AlmostExpired 	= Token + "alexp" + Token;
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
        Role 	cRole = Role.valueOf ((String)consumer.get (CommonSpec.Role));

		int 	offset 		= (Integer)request.get (Spec.Offset);
		int 	length 		= (Integer)request.get (Spec.Length);
		String 	sFilters 	= (String)request.get (Spec.Filters);
		
		SpaceKeyStore.ListFilter [] filters = null;
		if (!Lang.isNullOrEmpty (sFilters)) {
			String [] aFilters = Lang.split (sFilters, Lang.COMMA, true);
			filters = new SpaceKeyStore.ListFilter [aFilters.length + 1];
			for (int i = 0; i < aFilters.length; i++) {
				String f = aFilters [i];
				
				int idexOfStartUnderscore = f.indexOf (Token);
				if (idexOfStartUnderscore < -1) {
					continue;
				}
				
				int idexOfEndUnderscore = f.indexOf (Token, idexOfStartUnderscore + 2);
				if (idexOfEndUnderscore < -1) {
					continue;
				}
				
				filters [i] = new SpaceKeyStore.ListFilter () {
					@Override
					public String name () {
						return f.substring (0, idexOfStartUnderscore);
					}

					@Override
					public Object value () {
						String value = f.substring (idexOfEndUnderscore + 2);
						if (Lang.isNullOrEmpty (value)) {
							return null;
						}
						return value;
					}

					@Override
					public Operator operator () {
						try {
							return Operator.valueOf (f.substring (idexOfStartUnderscore + 2, idexOfEndUnderscore));
						} catch (Exception ex) {
							return Operator.eq;
						}
					}
					
				};
			}
		} else {
			 filters = new SpaceKeyStore.ListFilter [1];
		}
		
		JsonObject result = new JsonObject ();
		JsonArray aKeys = new JsonArray ();
		result.set (Output.Keys, aKeys);
		
		if (Role.SUPER.equals (cRole)) {
			
			filters [filters.length - 1] = new SpaceKeyStore.ListFilter () {
				@Override
				public String name () {
					return CommonSpec.Role;
				}

				@Override
				public Object value () {
					return Role.ADMIN.name ();
				}

				@Override
				public Operator operator () {
					return Operator.eq;
				}
			};
			
			try {
				Collection<ApiSpace> spaces = api.space ().spaces ();
				for (ApiSpace space : spaces) {
					addSpaceKeys (space, offset, length, filters, aKeys);
				}
			} catch (ApiAccessDeniedException e) {
				throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.NOT_FOUND);
			}
		} else {
			filters [filters.length - 1] = new SpaceKeyStore.ListFilter () {
				@Override
				public String name () {
					return CommonSpec.Role;
				}

				@Override
				public Object value () {
					return Role.DEVELOPER.name ();
				}

				@Override
				public Operator operator () {
					return Operator.eq;
				}
			};
			
			ApiSpace consumerSpace;
			try {
				consumerSpace = MgmUtils.space (consumer, api);
			} catch (ApiAccessDeniedException e) {
				throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.NOT_FOUND);
			}
			addSpaceKeys (consumerSpace, offset, length, filters, aKeys);
		}
		
		return new JsonApiOutput (result);
	}
	
	private void addSpaceKeys (ApiSpace space, int offset, int length, SpaceKeyStore.ListFilter [] filters, JsonArray aKeys) throws ApiServiceExecutionException {
		List<KeyPair> list = null;
		
		try {
			list = space.keystore ().list (offset, length, filters);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		if (list == null || list.isEmpty ()) {
			return;
		} 

		for (KeyPair keys : list) {
			aKeys.add (keys.toJson ());
		}
		
	}

}

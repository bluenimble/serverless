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
	
	interface Operators {
		String Equals 			= "__eq__";
		String NotEquals 		= "__neq__";
		String Like 			= "__like__";
		String Expired 			= "__exp__";
		String NotExpired 		= "__nexp__";
		String AlmostExpired 	= "__alexp__";
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		int 	offset 		= (Integer)request.get (Spec.Offset);
		int 	length 		= (Integer)request.get (Spec.Length);
		String 	sFilters 	= (String)request.get (Spec.Filters);
		
		SpaceKeyStore.ListFilter [] filters = null;
		if (!Lang.isNullOrEmpty (sFilters)) {
			String [] aFilters = Lang.split (sFilters, Lang.COMMA, true);
			filters = new SpaceKeyStore.ListFilter [aFilters.length];
			for (int i = 0; i < aFilters.length; i++) {
				String f = aFilters [i];
				
				int idexOfStartUnderscore = f.indexOf ("__");
				if (idexOfStartUnderscore < -1) {
					continue;
				}
				
				int idexOfEndUnderscore = f.indexOf ("__", idexOfStartUnderscore + 2);
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
		}
		
		ApiSpace consumerSpace;
		try {
			consumerSpace = MgmUtils.space (consumer, api);
		} catch (ApiAccessDeniedException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.NOT_FOUND);
		}
		
		JsonObject result = new JsonObject ();
		JsonArray aKeys = new JsonArray ();
		result.set (Output.Keys, aKeys);
		
		List<KeyPair> list = null;
		
		try {
			list = consumerSpace.keystore ().list (offset, length, filters);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		if (list == null || list.isEmpty ()) {
			return new JsonApiOutput (result);
		} 

		for (KeyPair keys : list) {
			aKeys.add (keys.toJson ());
		}

		return new JsonApiOutput (result);
	}

}

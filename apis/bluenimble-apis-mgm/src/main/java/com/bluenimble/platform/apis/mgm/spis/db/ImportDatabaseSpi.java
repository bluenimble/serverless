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
package com.bluenimble.platform.apis.mgm.spis.db;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.Database.ExchangeOption;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.storage.Storage;

public class ImportDatabaseSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	interface Spec {
		String Options 	= "options";
		String File		= "file";
		String Entities = "entities";
	}
	
	interface Output {
		String Feedback	= "feedback";
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		String 	provider 		= (String)request.get (CommonSpec.Provider);
		
		String 	[] aEntities 	= Lang.split ((String)request.get (Spec.Entities), Lang.COMMA, true);
		Set<String> entities = null;
		if (aEntities != null && aEntities.length > 0) {
			entities = new HashSet<String> ();
			for (String entity : aEntities) {
				entities.add (entity.toUpperCase ());
			}
		}
		
		String 	[] aOptions 	= Lang.split ((String)request.get (Spec.Options), Lang.COMMA, true);
		Map<ExchangeOption, Boolean> options = null;
		if (aOptions != null && aOptions.length > 0) {
			for (String o : aOptions) {
				try {
					options.put (ExchangeOption.valueOf (o.toLowerCase ()), true);
				} catch (Exception ex) {
					// ignore malformed options
				}
			}
		}
		
		ApiSpace space;
		try {
			space = MgmUtils.space (consumer, api);
		} catch (ApiAccessDeniedException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.FORBIDDEN);
		}
		
		String file = (String)request.get (Spec.File);
		
		JsonObject result = new JsonObject ();
		result.set (Spec.File, file);
		
		final StringBuilder sb = new StringBuilder ();
		
		try {
			space.feature (Database.class, provider, request).imp (
				entities, 
				space.feature (Storage.class, provider, request).root ().get (file).reader (request), 
				options, 
				new Database.ExchangeListener () {
					@Override
					public void onMessage (String message) {
						sb.append (message).append (Lang.ENDLN);
					}
				}
			);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		} 
		
		result.set (Output.Feedback, sb.toString ());
		
		sb.setLength (0);
		
		return new JsonApiOutput (result);
	}


}

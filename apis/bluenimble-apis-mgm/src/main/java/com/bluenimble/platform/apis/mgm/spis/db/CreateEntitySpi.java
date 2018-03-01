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

import com.bluenimble.platform.Json;
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
import com.bluenimble.platform.apis.mgm.CommonOutput;
import com.bluenimble.platform.apis.mgm.CommonSpec;
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.Database.Field;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class CreateEntitySpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;
	
	interface Spec {
		interface Field {
			String Name 	= "name";
			String Type 	= "type";
			String Required = "required";
			String Unique 	= "unique";
		}
	}

	@Override
	public ApiOutput execute (Api api, final ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		String 	provider 	= (String)request.get (CommonSpec.Provider);
		String 	sEntity 	= (String)request.get (CommonSpec.Entity);
		
		ApiSpace space;
		try {
			space = MgmUtils.space (consumer, api);
		} catch (ApiAccessDeniedException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.FORBIDDEN);
		}
		
		JsonObject payload 	= (JsonObject)request.get (ApiRequest.Payload); 
		
		JsonArray aFields 	= Json.getArray (payload, CommonSpec.Fields);
		if (aFields == null || aFields.isEmpty ()) {
			throw new ApiServiceExecutionException ("missing fields definition").status (ApiResponse.BAD_REQUEST);
		}
		
		Field [] fields = new Field [aFields.count ()];
		
		for (int i = 0; i < aFields.count (); i++) {
			Object o = aFields.get (i);
			if (o == null || !(o instanceof JsonObject)) {
				continue;
			}
			
			final JsonObject oField = (JsonObject)o;
			
			fields [i] = new Field () {
				@Override
				public String name () {
					return Json.getString (oField, Spec.Field.Name);
				}
				@Override
				public boolean required () {
					return Json.getBoolean (oField, Spec.Field.Required, false);
				}
				@Override
				public Type type () {
					try {
						return Type.valueOf (Json.getString (oField, Spec.Field.Type, Type.String.name ()));
					} catch (Exception ex) {
						return Type.String;
					}
				}
				@Override
				public boolean unique () {
					return Json.getBoolean (oField, Spec.Field.Unique, false);
				}
			};
			
		}
		
		try {
			Database db = space.feature (Database.class, provider, request);
			db.createEntity (sEntity, fields);
		} catch (DatabaseException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		} catch (Exception e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.NOT_FOUND);
		}
		
		return new JsonApiOutput ((JsonObject)new JsonObject ().set (CommonOutput.Dropped, true));
	}

}

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
import com.bluenimble.platform.apis.mgm.utils.MgmUtils;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

public class QueryEntitySpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -3682312790255625219L;

	interface Output {
		String Records 	= "records";
	}
	
	@Override
	public ApiOutput execute (Api api, final ApiConsumer consumer, ApiRequest request,
			ApiResponse response) throws ApiServiceExecutionException {
		
		String 	provider 	= (String)request.get (CommonSpec.Provider);
		String 	sEntity 	= (String)request.get (CommonSpec.Entity);
		
		String 	serializer 	= (String)request.get (CommonSpec.Serializer);
		
		String [] levels = Lang.split (serializer, Lang.COMMA);
		
		int allStopLevel = 2;
		if (levels != null && levels.length > 0) {
			try { allStopLevel = Integer.valueOf (levels [0]); } catch (Exception ex) {}
		}
		int minStopLevel = 3;
		if (levels != null && levels.length > 0) {
			try { minStopLevel = Integer.valueOf (levels [1]); } catch (Exception ex) {}
		}
		
		
		JsonObject payload = (JsonObject)request.get (ApiRequest.Payload); 
		payload.set (Database.Fields.Entity, sEntity);
		
		ApiSpace space;
		try {
			space = MgmUtils.space (consumer, api);
		} catch (ApiAccessDeniedException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e).status (ApiResponse.FORBIDDEN);
		}
		
		List<DatabaseObject> records = null;
		
		try {
			Database db = space.feature (Database.class, provider, request);
			records = db.find (null, new JsonQuery (payload), null);
		} catch (DatabaseException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		} 
		
		JsonObject result = new JsonObject ();
		JsonArray aRecords = new JsonArray ();
		result.set (Output.Records, aRecords);
		
		if (records == null || records.isEmpty ()) {
			return new JsonApiOutput (result);
		}
		
		for (int i = 0; i < records.size (); i++) {
			aRecords.add (records.get (i).toJson (new DefaultBeanSerializer (allStopLevel, minStopLevel)));
		}
		
		return new JsonApiOutput (result).set (ApiOutput.Defaults.Cast, true);
	}

}

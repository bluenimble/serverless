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
package com.bluenimble.platform.api.impls.im;

import java.util.Date;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.SimpleApiServiceSpi;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Config;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Defaults;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Fields;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Spec;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.impls.DefaultDatabaseObjectSerializer;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.impls.JsonQuery;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.im.SecurityUtils;

public class ActivateServiceSpi extends SimpleApiServiceSpi {

	private static final long serialVersionUID = -5297356423303847595L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject config = request.getService ().getCustom ();
		
		Database db = api.space ().feature (Database.class, Json.getString (config, Config.Database, ApiSpace.Features.Default), request);
		
		DatabaseObject account = null;
		try {
			JsonObject query = Json.getObject (config, Config.Query);
			if (query == null) {
				query = new JsonObject ();
				
				JsonObject where = new JsonObject ();
				query.set (Query.Construct.where.name (), where);
				
				where.set (Json.getString (config, Config.ActivationCodeProperty, Defaults.ActivationCode), request.get (Spec.ActivationCode));
			}
			
			account = db.findOne (Json.getString (config, Config.UsersEntity, Defaults.Users), new JsonQuery (query));
			
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		if (account == null) {
			throw new ApiServiceExecutionException ("account not found").status (ApiResponse.NOT_FOUND);
		}
		
		Date now = new Date ();

		// remove activationCode
		account.remove (Json.getString (config, Config.ActivationCodeProperty, Defaults.ActivationCode));
		
		// update lastLogin
		try {
			account.set (Json.getString (config, Config.LastLoginProperty, Fields.LastLogin), now);
			account.save ();
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		JsonObject oAccount = account.toJson (DefaultDatabaseObjectSerializer.Default);
		
		// create token
		String [] tokenAndExpiration = SecurityUtils.tokenAndExpiration (api, oAccount, now);

		oAccount.remove (Json.getString (config, Config.PasswordProperty, Spec.Password));
		oAccount.set (Defaults.Token, tokenAndExpiration [0]);
		oAccount.set (Defaults.ExpiresOn, tokenAndExpiration [1]);
		
		// call onFinish if any
		JsonObject onFinish = Json.getObject (config, Config.onFinish.class.getSimpleName ());
		ApiOutput onFinishOutput = SecurityUtils.onFinish (api, consumer, request, onFinish, oAccount);
		if (onFinishOutput != null) {
			oAccount.set (
				Json.getString (onFinish, Config.onFinish.ResultProperty, Config.onFinish.class.getSimpleName ()),
				onFinishOutput.data ()
			);
		}

		return new JsonApiOutput (oAccount);
	}

}
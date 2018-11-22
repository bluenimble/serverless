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

import com.bluenimble.platform.Crypto;
import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Config;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Defaults;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Spec;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.im.SecurityUtils;

public class ChangePasswordSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -5297356423303847595L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject config = request.getService ().getSpiDef ();
		
		Database db = feature (api, Database.class, Json.getString (config, Config.Database, ApiSpace.Features.Default), request).trx ();
		
		DatabaseObject account = null;
		try {
			account = db.get (Json.getString (config, Config.UsersEntity, Defaults.User), (String)consumer.get (ApiConsumer.Fields.Id));
			account.set (
				Json.getString (config, Config.PasswordProperty, Spec.Password), 
				Crypto.md5 ((String)request.get (Spec.Password), Encodings.UTF8)
			);
			account.save ();
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		ApiOutput result = new JsonApiOutput (JsonObject.Blank);
		
		// call extend if any
		if (config.containsKey (Config.onFinish.class.getSimpleName ())) {
			JsonObject oAccount = account.toJson (null);
			result = SecurityUtils.onFinish (api, consumer, request, Json.getObject (config, Config.onFinish.class.getSimpleName ()), oAccount);
		}
		
		return result;
	}

}
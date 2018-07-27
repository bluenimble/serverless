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
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.impls.JsonQuery;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.im.SecurityUtils;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

public class LoginServiceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -5297356423303847595L;
	
	interface ActivationCodeTypes {
		String UUID = "uuid";
		String CPIN = "cpin"; // 8
		String NPIN = "npin"; // 8
	}
	
	interface Defaults {
		String 	User 			= "User";
		String 	ActivationCode	= "activationCode";
	}

	public interface Config {
		String Database 				= "database";
		String Query					= "query";
		String RequiresActivation		= "requiresActivation";
		String UseUserAsEmailAddress	= "useUserAsEmailAddress";
		
		String ActivationCodeType 		= "activationCodeType";
		String PinLength 				= "pinLength";
		
		String EncryptPassword			= "encryptPassword";

		String UsersEntity				= "usersEntity"; 

		String UserProperty				= "userProperty";
		String PasswordProperty			= "passwordProperty"; 
		String ActivationCodeProperty	= "activationCodeProperty";
		String LastLoginProperty		= "lastLoginProperty";
		
		String SignupEmail				= "email";
		
		String Data 					= "data";
		
		interface onFinish	{
			String Space 	= "space";
			String Api 		= "api";
			String Verb 	= "verb";
			String Resource = "resource";
			String ResultProperty
							= "resultProperty";
		}
	}
	
	interface Spec {
		String 	User 			= "user";
		String 	Password 		= "password";
		String 	Email 			= "email";
		
		String  ActivationCode	= "activationCode";
	}

	interface Fields {
		String 	Email 			= "email";
		String 	Password 		= "password";
		String 	LastLogin 		= "lastLogin";
	}

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject config = request.getService ().getSpiDef ();
		
		JsonObject payload = (JsonObject)request.get (ApiRequest.Payload);
		
		boolean encryptPassword = Json.getBoolean (config, Config.EncryptPassword, true);
		
		Database db = feature (api, Database.class, Json.getString (config, Config.Database, ApiSpace.Features.Default), request).trx ();
		
		DatabaseObject account = null;
		try {
			JsonObject query = Json.getObject (config, Config.Query);
			if (query == null) {
				query = new JsonObject ();
				
				JsonObject where = new JsonObject ();
				query.set (Query.Construct.where.name (), where);
				
				where.set (Json.getString (config, Config.UserProperty, Fields.Email), payload.get (Spec.User));
				where.set (
					Json.getString (config, Config.PasswordProperty, Fields.Password), 
					encryptPassword ? Crypto.md5 (Json.getString (payload, Spec.Password), Encodings.UTF8) : Json.getString (payload, Spec.Password)
				);
			}
						
			account = db.findOne (Json.getString (config, Config.UsersEntity, Defaults.User), new JsonQuery (query));
			
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		if (account == null) {
			throw new ApiServiceExecutionException ("account not found").status (ApiResponse.UNAUTHORIZED);
		}
		
		boolean active = true;
		
		boolean requiresActivation = Json.getBoolean (config, Config.RequiresActivation, false);
		
		if (requiresActivation && account.get (Json.getString (config, Config.ActivationCodeProperty, Defaults.ActivationCode)) != null) {
			active = false;
		}
		
		JsonObject oAccount = account.toJson (DefaultBeanSerializer.Default);
		
		oAccount.remove (Json.getString (config, Config.PasswordProperty, Spec.Password));

		if (active) {
			Date now = new Date ();
			
			// update lastLogin
			try {
				account.set (Json.getString (config, Config.LastLoginProperty, Fields.LastLogin), now);
				account.save ();
			} catch (Exception ex) {
				throw new ApiServiceExecutionException (ex.getMessage (), ex);
			}
			
			// create token
			String [] tokenAndExpiration = SecurityUtils.tokenAndExpiration (api, oAccount, now, 0);
			oAccount.set (ApiConsumer.Fields.Token, tokenAndExpiration [0]);
			oAccount.set (ApiConsumer.Fields.ExpiryDate, tokenAndExpiration [1]);
		} 
		
		// call extend if any
		JsonObject onFinish = Json.getObject (config, Config.onFinish.class.getSimpleName ());
		
		ApiOutput onFinishOutput = SecurityUtils.onFinish (api, consumer, request, onFinish, oAccount);
		
		oAccount.remove (Database.Fields.Id);
		
		if (onFinishOutput != null) {
			oAccount.set (
				Json.getString (onFinish, Config.onFinish.ResultProperty, Config.onFinish.class.getSimpleName ()),
				onFinishOutput.data ()
			);
		}
		
		return new JsonApiOutput (oAccount);
	}
	
}
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
import java.util.Iterator;

import com.bluenimble.platform.Crypto;
import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.im.SecurityUtils;
import com.bluenimble.platform.query.Query;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.reflect.beans.BeanSerializer;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

public class LoginServiceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -5297356423303847595L;
	
	public static final BeanSerializer BeanSerializer = new DefaultBeanSerializer (1, 1);
	
	private static final String AccountNotFound				= "AccountNotFound";
	private static final String OrganizationNotFound		= "OrganizationNotFound";
	private static final String OrganizationLinkNotFound	= "OrganizationLinkNotFound";

	interface ActivationCodeTypes {
		String UUID = "uuid";
		String CPIN = "cpin"; // 8
		String NPIN = "npin"; // 8
	}
	
	interface Defaults {
		String 	Organization	= "Organization";	
		String 	User 			= "User";
		String 	OrganizationUser= "OrganizationUser";	
		String 	ActivationCode	= "activationCode";
		String 	This			= "this";
		String	Yes				= "y";
		String	No				= "n";
		String	TokenType		= "standard";
	}

	public interface Config {
		String Lookup					= "lookup";
		String Check					= "check";
		String Database 				= "database";
		String Entity					= "entity";
		String Query					= "query";
		String Copy						= "copy";
		String Organization				= "organization";
		String IfPresent				= "ifPresent";
		String RequiresActivation		= "requiresActivation";
		String UseUserAsEmailAddress	= "useUserAsEmailAddress";
		
		String ActivationCodeType 		= "activationCodeType";
		String PinLength 				= "pinLength";
		
		String EnableScripting			= "enableScripting";
		
		String EncryptPassword			= "encryptPassword";

		String UsersEntity				= "usersEntity"; 

		String UserProperty				= "userProperty";
		String PasswordProperty			= "passwordProperty"; 
		String ActivationCodeProperty	= "activationCodeProperty";
		String LastLoginProperty		= "lastLoginProperty";
		
		String SignupEmail				= "email";
		
		String Data 					= "data";
		
		String Owner					= "owner";
		
		interface onFinish	{
			String ResultProperty = "resultProperty";
		}
	}
	
	interface Spec {
		String 	User 			= "user";
		String 	Password 		= "password";
		String 	Email 			= "email";
		String 	TokenAge 		= "tokenAge";
		String 	TokenType		= "tokenType";
		
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
				if (!request.getChannel ().equals (ApiRequest.Channels.container.name ())) {
					where.set (
						Json.getString (config, Config.PasswordProperty, Fields.Password), 
						encryptPassword ? Crypto.md5 (Json.getString (payload, Spec.Password), Encodings.UTF8) : Json.getString (payload, Spec.Password)
					);
				}
			} else {
				query = (JsonObject)Json.template (query, payload, false);
			}
						
			account = db.findOne (Json.getString (config, Config.UsersEntity, Defaults.User), new JsonQuery (query));
			
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		if (account == null) {
			String message = api.message (request.getLang (), AccountNotFound);
			if (message == null || message.equals (AccountNotFound)) {
				message = "Account not found";
			}
			throw new ApiServiceExecutionException (message).status (ApiResponse.UNAUTHORIZED);
		}
		
		boolean active = true;
		
		boolean requiresActivation = Json.getBoolean (config, Config.RequiresActivation, false);
		
		if (requiresActivation && account.get (Json.getString (config, Config.ActivationCodeProperty, Defaults.ActivationCode)) != null) {
			active = false;
		}
		
		JsonObject oAccount = account.toJson (BeanSerializer);
		
		oAccount.remove (Json.getString (config, Config.PasswordProperty, Spec.Password));
		
		// generic copy
		JsonObject oCopy = Json.getObject (config, Config.Copy);
		if (oCopy != null) {
			Iterator<String> keys = oCopy.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				Json.set (oAccount, Json.getString (oCopy, key), Json.find (oAccount, Lang.split (key, Lang.DOT)));
			}
		}

		JsonObject 	org 				= Json.getObject (config, Config.Organization);
		String 		ifPresentField 		= (String)Json.find (config, Config.Organization, Config.IfPresent);
		
		if (org != null && payload.get (ifPresentField) != null) {
			payload.set (Spec.User, account.getId ());
			JsonObject lookup 		= Json.getObject (org, Config.Lookup);
			JsonObject lookupQuery 	= Json.getObject (lookup, Config.Query);
			JsonObject check 		= Json.getObject (org, Config.Check);
			JsonObject checkQuery 	= Json.getObject (check, Config.Query);
			
			if (lookupQuery != null && checkQuery != null) {
				lookupQuery = (JsonObject)Json.template (lookupQuery, payload, false);
				checkQuery = (JsonObject)Json.template (checkQuery, payload, false);

				DatabaseObject oOrg = null;
				try {
					oOrg = db.findOne (Json.getString (lookup, Config.Entity, Defaults.Organization), new JsonQuery (lookupQuery));
				} catch (Exception ex) {
					throw new ApiServiceExecutionException (ex.getMessage (), ex);
				}
				if (oOrg == null) {
					String message = api.message (request.getLang (), OrganizationNotFound);
					if (message == null || message.equals (OrganizationNotFound)) {
						message = "Organization not found";
					}
					throw new ApiServiceExecutionException (message).status (ApiResponse.UNAUTHORIZED);
				}
				
				// check if part of this org
				DatabaseObject checkRecord = null;
				try {
					checkRecord = db.findOne (Json.getString (check, Config.Entity, Defaults.Organization), new JsonQuery (checkQuery));
				} catch (Exception ex) {
					throw new ApiServiceExecutionException (ex.getMessage (), ex);
				}
				if (checkRecord == null) {
					String message = api.message (request.getLang (), OrganizationLinkNotFound);
					if (message == null || message.equals (OrganizationLinkNotFound)) {
						message = "Organization link not found";
					}
					throw new ApiServiceExecutionException (message).status (ApiResponse.UNAUTHORIZED);
				}
				
				// lookup copy
				oCopy = Json.getObject (lookup, Config.Copy);
				if (oCopy != null) {
					JsonObject jOrg = oOrg.toJson (BeanSerializer);
					Iterator<String> keys = oCopy.keys ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						Object value = null;
						if (key.equals (Defaults.This)) {
							value = jOrg;
						} else {
							value = Json.find (jOrg, Lang.split (key, Lang.DOT));
						}
						Json.set (oAccount, Json.getString (oCopy, key), value);
					}
				}
				
				// check copy
				oCopy = Json.getObject (check, Config.Copy);
				if (oCopy != null) {
					JsonObject jCheck = checkRecord.toJson (BeanSerializer);
					Iterator<String> keys = oCopy.keys ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						Object value = null;
						if (key.equals (Defaults.This)) {
							value = jCheck;
						} else {
							value = Json.find (jCheck, Lang.split (key, Lang.DOT));
						}
						Json.set (oAccount, Json.getString (oCopy, key), value);
					}
				}
			}
			oAccount.set (Config.Owner, Defaults.No);
		} else {
			if (request.getChannel ().equals (ApiRequest.Channels.container.name ()) && payload.containsKey (Config.Owner)) {
				oAccount.set (Config.Owner, Json.getBoolean (payload, Config.Owner, false) ? Defaults.Yes : Defaults.No);
			} else {
				oAccount.set (Config.Owner, Defaults.Yes);
			}
		}
		
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
			long age = 0;
			String tokenType = Defaults.TokenType;
			if (request.getChannel ().equals (ApiRequest.Channels.container.name ())) {
				age = Json.getLong (payload, Spec.TokenAge, 0);
				tokenType = Json.getString (payload, Spec.TokenType, Defaults.TokenType);
			}
			oAccount.set (ApiConsumer.Fields.TokenType, tokenType);
			String [] tokenAndExpiration = SecurityUtils.tokenAndExpiration (api, oAccount, now, age);
			oAccount.set (ApiConsumer.Fields.Token, tokenAndExpiration [0]);
			oAccount.set (ApiConsumer.Fields.ExpiryDate, tokenAndExpiration [1]);
		} 
		
		// call onFinish if any
		return SecurityUtils.onFinish (
			api, consumer, request, 
			Json.getObject (config, Config.onFinish.class.getSimpleName ()), 
			oAccount
		);
	}
	
}
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
import java.util.concurrent.Callable;

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
import com.bluenimble.platform.api.CodeExecutor;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.SimpleApiServiceSpi;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.ActivationCodeTypes;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Config;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Defaults;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Fields;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Spec;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.impls.JsonQuery;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.messaging.Messenger;
import com.bluenimble.platform.messaging.impls.JsonActor;
import com.bluenimble.platform.messaging.impls.JsonRecipient;
import com.bluenimble.platform.messaging.impls.JsonSender;
import com.bluenimble.platform.plugins.im.SecurityUtils;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

public class SignupServiceSpi extends SimpleApiServiceSpi {

	private static final long serialVersionUID = -5297356423303847595L;
	
	interface Email {
		String Messenger 	= "messenger";
		String FromEmail 	= "fromEmail";
		String FromName 	= "fromName";
		String Subject 		= "subject";
		String Template 	= "template";
	}

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject config = request.getService ().getCustom ();
		
		JsonObject payload = (JsonObject)request.get (ApiRequest.Payload);

		Database db = api.space ().feature (Database.class, Json.getString (config, Config.Database, ApiSpace.Features.Default), request);
		
		DatabaseObject account = null;
		try {
			
			JsonObject where = null;
			
			JsonObject query = Json.getObject (config, Config.Query);
			if (query == null) {
				query = new JsonObject ();
				where = new JsonObject ();
				query.set (Query.Construct.where.name (), where);
			} else {
				where = Json.getObject (query, Query.Construct.where.name ());
			}
			
			query.set (Database.Fields.Entity, Json.getString (config, Config.UsersEntity, Defaults.User));

			where.set (Json.getString (config, Config.UserProperty, Fields.Email), Json.getString (payload, Spec.User));
			
			account = db.findOne (null, new JsonQuery (query));
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		if (account != null) {
			throw new ApiServiceExecutionException ("account already exists").status (ApiResponse.CONFLICT);
		}
		
		boolean requiresActivation = Json.getBoolean (config, Config.RequiresActivation, false);
		
		try {
			account = db.create (Json.getString (config, Config.UsersEntity, Defaults.User));
			
			account.load (payload);
			
			// set user property
			account.set (Json.getString (config, Config.UserProperty, Fields.Email), Json.getString (payload, Spec.User));
			account.remove (Spec.User);
			
			boolean encryptPassword = Json.getBoolean (config, Config.EncryptPassword, true);
		
			account.set (Json.getString (config, Config.PasswordProperty, Spec.Password), encryptPassword ? Crypto.md5 (Json.getString (payload, Spec.Password), Encodings.UTF8) : Json.getString (payload, Spec.Password));
			
			JsonObject extraData = Json.getObject (config, Config.Data);
			if (extraData != null && !extraData.isEmpty ()) {
				Iterator<String> keys = extraData.keys ();
				while (keys.hasNext ()) {
					String key = keys.next ();
					account.set (key, extraData.get (key));
				}
			}
			
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		try {
			String activationCode = null;
			if (requiresActivation) {
				String acType = Json.getString (config, Config.ActivationCodeType, ActivationCodeTypes.CPIN).toLowerCase ();
				int pinLength = Json.getInteger (config, Config.PinLength, 6);
				if (acType.equals (ActivationCodeTypes.CPIN)) {
					activationCode = Lang.UUID (pinLength);
				} else if (acType.equals (ActivationCodeTypes.NPIN)) {
					activationCode = Lang.pin (pinLength);
				} else {
					activationCode = Lang.rand ();
				}
				account.set (Json.getString (config, Config.ActivationCodeProperty, Defaults.ActivationCode), activationCode);
			}
			account.save ();
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		payload.remove (Spec.Password);
		
		JsonObject result = account.toJson (DefaultBeanSerializer.Default);
		
		String email = Json.getString (payload, Spec.Email); 
		if (Lang.isNullOrEmpty (email)) {
			if (Json.getBoolean (config, Config.UseUserAsEmailAddress, false)) {
				email = Json.getString (payload, Spec.User);
			}
		}
		
		result.remove (Json.getString (config, Config.PasswordProperty, Spec.Password));
		
		if (!requiresActivation || Lang.isNullOrEmpty (email)) {
			Date now = new Date ();
			// update lastLogin
			try {
				account.set (Json.getString (config, Config.LastLoginProperty, Fields.LastLogin), now);
				account.save ();
			} catch (Exception ex) {
				throw new ApiServiceExecutionException (ex.getMessage (), ex);
			}

			// create token
			String [] tokenAndExpiration = SecurityUtils.tokenAndExpiration (api, result, now, 0);
			result.set (ApiConsumer.Fields.Token, tokenAndExpiration [0]);
			result.set (ApiConsumer.Fields.ExpiryDate, tokenAndExpiration [1]);
			
			return new JsonApiOutput (result);
		}
		
		// requires activation and email is present in payload
		
		JsonObject 	oEmail  	= Json.getObject (config, Config.SignupEmail);
		String 		feature 	= Json.getString (oEmail, Email.Messenger);
		String 		template 	= Json.getString (oEmail, Email.Template);
		
		if (oEmail != null && !Lang.isNullOrEmpty (feature) && !Lang.isNullOrEmpty (template)) {
			
			String 		fromEmail 	= Json.getString (oEmail, Email.FromEmail);
			String 		fromName	= Json.getString (oEmail, Email.FromName);
			String 		subject 	= Json.getString (oEmail, Email.Subject, "Welcome to " + api.getName ());
			
			final Messenger messenger = api.space ().feature (Messenger.class, feature, request);
			
			final JsonObject emailTemplateData = account.toJson (null);
			
			try {
				
				final String fEmail = email;
				
				api.space ().executor ().execute (new Callable<Void> () {
					@Override
					public Void call () {
						try {
							messenger.send (
								new JsonSender ((JsonObject)new JsonObject ().set (JsonActor.Spec.Id, fromEmail).set (JsonActor.Spec.Name, fromName)),
								new JsonRecipient [] {
									new JsonRecipient ((JsonObject)new JsonObject ().set (JsonActor.Spec.Id, fEmail))
								},
								subject,
								api.getResourcesManager ().get (Lang.split (template, Lang.SLASH)),
								emailTemplateData
							);	
						} catch (Exception ex) {
							throw new RuntimeException (ex.getMessage (), ex);
						}
				        return null;
					}
				}, CodeExecutor.Mode.Async);
			
			} catch (Exception ex) {
				throw new ApiServiceExecutionException (ex.getMessage (), ex);
			}
		}
		
		// call extend if any
		JsonObject onFinish = Json.getObject (config, Config.onFinish.class.getSimpleName ());
		ApiOutput onFinishOutput = SecurityUtils.onFinish (api, consumer, request, onFinish, result);
		
		if (onFinishOutput != null) {
			result.set (
				Json.getString (onFinish, Config.onFinish.ResultProperty, Config.onFinish.class.getSimpleName ()),
				onFinishOutput.data ()
			);
		}
		
		return new JsonApiOutput (result);
	}

}
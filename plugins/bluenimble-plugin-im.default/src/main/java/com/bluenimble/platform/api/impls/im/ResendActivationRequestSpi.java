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

import java.util.concurrent.Callable;

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
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Config;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Defaults;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Fields;
import com.bluenimble.platform.api.impls.im.LoginServiceSpi.Spec;
import com.bluenimble.platform.api.impls.im.SignupServiceSpi.Email;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.messaging.Messenger;
import com.bluenimble.platform.messaging.impls.JsonActor;
import com.bluenimble.platform.messaging.impls.JsonRecipient;
import com.bluenimble.platform.messaging.impls.JsonSender;
import com.bluenimble.platform.query.Query;
import com.bluenimble.platform.query.impls.JsonQuery;

public class ResendActivationRequestSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -5297356423303847595L;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		JsonObject config = request.getService ().getSpiDef ();
		
		Database db = feature (api, Database.class, Json.getString (config, Config.Database, ApiSpace.Features.Default), request);
		
		DatabaseObject account = null;
		try {
			JsonObject query = Json.getObject (config, Config.Query);
			if (query == null) {
				query = new JsonObject ();
				
				JsonObject where = new JsonObject ();
				query.set (Query.Construct.where.name (), where);
				
				where.set (Json.getString (config, Config.UserProperty, Fields.Email), request.get (Spec.Email));
			}
						
			account = db.findOne (Json.getString (config, Config.UsersEntity, Defaults.User), new JsonQuery (query));
			
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		if (account == null) {
			throw new ApiServiceExecutionException ("account not found").status (ApiResponse.NOT_FOUND);
		}

		String email = (String)account.get (Json.getString (config, Config.UserProperty, Fields.Email));
		
		if (Lang.isNullOrEmpty (email)) {
			throw new ApiServiceExecutionException ("user email not found").status (ApiResponse.NOT_FOUND);
		}
		
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
		
		return new JsonApiOutput (JsonObject.Blank);
	}

}
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
package com.bluenimble.platform.icli.mgm;

import java.io.Serializable;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;

public class Keys implements Serializable {

	private static final long serialVersionUID = 7849960629954968022L;
	
	interface Spec {
		String Name 		= "name";
		String Domain 		= "domain";
		String Issuer 		= "issuer";
		String WhenIssued 	= "whenIssued";
		String Endpoint 	= "endpoint";
		String Space 		= "space";
		String User 		= "user";
			String Id 		= "id";
			String Stamp 	= "stamp";
	}
	
	private String 		alias;

	private JsonObject 	source;
	
	public Keys (String alias, JsonObject source) {
		this.alias 	= alias;
		this.source = source;
	}
	
	public String alias () {
		return alias;
	}
	
	public String name () {
		return Json.getString (source, Spec.Name);
	}

	public String issuer () {
		return Json.getString (source, Spec.Issuer);
	}

	public String whenIssued () {
		return Json.getString (source, Spec.WhenIssued);
	}

	public String expiresOn () {
		return Json.getString (source, KeyPair.Fields.ExpiryDate);
	}

	public String user () {
		return (String)Json.find (source, Spec.User, Spec.Id);
	}

	public String stamp () {
		return (String)Json.find (source, Spec.User, Spec.Stamp);
	}

	public String endpoint () {
		return Json.getString (source, Spec.Endpoint);
	}
	public String domain () {
		return Json.getString (source, Spec.Domain);
	}
	public String space () {
		return Json.getString (source, Spec.Space);
	}
	public String accessKey () {
		return Json.getString (source, KeyPair.Fields.AccessKey);
	}
	public String secretKey () {
		return Json.getString (source, KeyPair.Fields.SecretKey);
	}
	
	public JsonObject json () {
		return source;
	}
	
}

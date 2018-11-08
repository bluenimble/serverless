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
package com.bluenimble.platform.api.security;

import com.bluenimble.platform.Referenceable;
import com.bluenimble.platform.json.JsonObject;

public interface ApiConsumer extends Referenceable {

	String Anonymous = "anonymous";

	enum Type {
		Token,
		Signature,
		Basic,
		Cookie,
		Unknown
	}
	
	interface Fields {
		String Type 		= "type";
		String Id 			= "id";
		String Owner 		= "owner";
		
		String Token 		= "token";

		String AccessKey 	= "accessKey";
		String SecretKey 	= "secretKey";
		String Signature 	= "signature";
		String ExpiryDate 	= "expiryDate";
		
		String Password 	= "password";
		
		String Space 		= "space";
		
		String Role 		= "role";
		String Permissions 	= "permissions";

		String Anonymous	= "anonymous";
	}
	
	Type 		type ();

	Object 		get (String... property);
	void 		set (String property, Object value);
	
	JsonObject 	toJson ();
	
	void		override (ApiConsumer consumer);
	
	boolean		isAnonymous ();

}

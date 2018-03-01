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
package com.bluenimble.platform.cache;

import java.io.Serializable;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.json.JsonObject;

@Feature ( name = "cache" )
public interface Cache extends Serializable {

	JsonObject 	list 	();
	
	void 		create 	(String bucket, long ttl);
	boolean 	exists 	(String bucket);
	void 		drop 	(String bucket);
	void 		clear 	(String bucket);
	JsonObject 	get 	(String bucket, int start, int page);
	
	void 		put 	(String bucket, String key, Object value, long ttl);
	Object 		get 	(String bucket, String key, boolean remove);
	void 		delete 	(String bucket, String key);
	
}

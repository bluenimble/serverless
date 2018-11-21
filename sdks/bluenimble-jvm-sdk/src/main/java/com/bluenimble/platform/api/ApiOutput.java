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
package com.bluenimble.platform.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

public interface ApiOutput extends Serializable {
	
	interface Defaults {
		String Id 			= "id";
		String Timestamp 	= "timestamp";
		String Cache 		= "cache";
		String Disposition 	= "disposition";
		String Charset 		= "charset";
		String Items 		= "items";
		String Count 		= "count";
		String Expires		= "expires";
		String Cast			= "cast";
	}

	interface Disposition 		{
		String Inline 		= "inline";
		String Attachment 	= "attachment";
	}

	String 				name 		();
	Date 				timestamp 	();
	String 				contentType ();

	String 				extension 	();
	long 				length 		();
	
	ApiOutput 			set 		(String key, Object value);
	ApiOutput 			unset 		(String key);
	Object 				get 		(String key);

	JsonObject 			meta 		();
	JsonObject 			data 		();

	void 				pipe 		(OutputStream out, long position, long count) 	throws IOException;
	InputStream 		toInput 	() 												throws IOException;
	
	void 				pipe 		(OutputStream out, StreamDecorator decorator, Chunk... chunks) 			
																					throws IOException;

}

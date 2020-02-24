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
package com.bluenimble.platform.storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.Channel;
import java.nio.file.OpenOption;
import java.util.Date;

import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

public interface StorageObject extends Serializable {

	public interface Fields {
		String Name 		= "name";
		String Timestamp 	= "timestamp";
		String Length 		= "length";
		String ContentType	= "contentType";
	}

	String 		name 		();
	Date 		timestamp 	();
	
	long 		length 		() 											throws StorageException;
	String 		contentType ();

	long		count		();
	boolean		exists		();

	boolean 	isFolder 	();

	void 		rename 		(String nn)									throws StorageException;

	boolean 	delete 		() 											throws StorageException;
	long		update		(InputStream payload, boolean append)		throws StorageException;
	void		truncate	()											throws StorageException;
	
	void 		copy 		(Folder folder, boolean move, String altName) 
																		throws StorageException;
	
	JsonObject 	toJson 		();

	void 		pipe 		(OutputStream out, 	long position, long count) 	
																		throws StorageException;
	void 		pipe 		(InputStream in, 	long position, long count) 	
																		throws StorageException;
	void 		pipe 		(OutputStream out, StreamDecorator decorator, Chunk... chunks) 			
																		throws StorageException;
	
	ApiOutput	toOutput	(Folder.Filter filter, String altName, String altContentType)		
																		throws StorageException;
	
	ApiStreamSource	
				toStreamSource	
							(String altName, String altContentType)		throws StorageException;
	OutputStream 
				writer		(ApiContext context)						throws StorageException;
	
	InputStream reader		(ApiContext context)						throws StorageException;

	Channel 	channel		(OpenOption... options)						throws StorageException;

}

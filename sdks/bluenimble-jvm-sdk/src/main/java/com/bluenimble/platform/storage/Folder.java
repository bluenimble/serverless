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

import com.bluenimble.platform.api.ApiStreamSource;

public interface Folder extends StorageObject {

	interface Filter {
		boolean accept 	(StorageObject so);
	}
	
	interface Visitor {
		void 	visit 	(StorageObject so);
	}
	
	// creates a folder
	Folder 			add 			(String name, boolean ignoreIfExist)	throws StorageException;
	
	// creates a stream aware object / file
	StorageObject 	add 			(ApiStreamSource ss, String altName, boolean overwrite)	
																			throws StorageException;
	
	StorageObject 	get 			(String path)							throws StorageException;
	
	void			list 			(Visitor visitor, Filter filter)		throws StorageException;
	
	boolean			contains		(String path)							throws StorageException;
	
}

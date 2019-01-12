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
package com.bluenimble.platform.storage.impls;

import java.io.File;

import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.Storage;
import com.bluenimble.platform.storage.StorageException;

public class FileSystemStorage implements Storage {

	private static final long serialVersionUID = 9208848890318179761L;

	protected 	Folder 	root;
	protected 	int 	buffer;
	
	public FileSystemStorage (File mount, int buffer) {
		this.buffer = buffer;
		this.root = new FileSystemFolder (mount, true, this.buffer);
	}

	@Override
	public long quota () throws StorageException {
		return -1;
	}

	@Override
	public Folder root () throws StorageException {
		return root;
	}

}

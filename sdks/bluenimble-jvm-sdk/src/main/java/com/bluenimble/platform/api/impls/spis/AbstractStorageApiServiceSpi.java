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
package com.bluenimble.platform.api.impls.spis;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.storage.StorageObject;

public abstract class AbstractStorageApiServiceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = 1283296736684087088L;
	
	protected String provider = ApiSpace.Features.Default;
	protected String folder;
	protected String objectParameter = "file";

	protected Folder findFolder (Folder parent, String path) throws StorageException {
		Folder folder = parent;
		if (Lang.isNullOrEmpty (path)) {
			return folder;
		}
		StorageObject so = folder.get (path);
		if (so == null) {
			throw new StorageException (path + " not found");
		}
		if (!so.isFolder ()) {
			throw new StorageException (path + " isn't a valid folder");
		}
		return (Folder)so;
	}
	
	public String getProvider () {
		return provider;
	}
	public void setProvider (String provider) {
		this.provider = provider;
	}

	public String getObjectParameter () {
		return objectParameter;
	}
	public void setObjectParameter (String objectParameter) {
		this.objectParameter = objectParameter;
	}
	
	public String getFolder () {
		return folder;
	}
	public void setFolder (String folder) {
		this.folder = folder;
	}
}
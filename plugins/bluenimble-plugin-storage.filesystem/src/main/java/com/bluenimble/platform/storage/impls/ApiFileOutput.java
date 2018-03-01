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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.media.MediaTypeUtils;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

public class ApiFileOutput implements ApiOutput {

	private static final long serialVersionUID = -4371715321710893775L;
	
	private FileSystemStorageObject 	object;
	private String 						name;
	private String 						contentType;
	private String 						extension;
	private Folder.Filter 				filter;
	private JsonObject					meta;
	
	public ApiFileOutput (FileSystemStorageObject object, Folder.Filter filter, String altName, String altContentType) {
		this.object 		= object;
		this.filter 		= filter;
		this.name 			= altName != null ? altName : object.name ();
		this.contentType 	= altContentType;
		
		int indexOfDot = name.lastIndexOf (Lang.DOT);
		if (indexOfDot >= 0) {
			extension = name.substring (indexOfDot + 1);
		}
		
		if (!object.isFolder ()) {
			this.meta = new JsonObject ();
			this.meta.set (ApiOutput.Defaults.Timestamp, object.timestamp ());
		}
	}
	
	@Override
	public JsonObject data () {
		return object.toJson (filter, true);
	}
	
	@Override
	public JsonObject meta () {
		return meta;
	}

	@Override
	public long length () {
		try {
			return object.length ();
		} catch (StorageException e) {
			throw new RuntimeException (e.getMessage (), e);
		}	
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public String extension () {
		return extension;
	}

	@Override
	public String contentType () {
		if (!Lang.isNullOrEmpty (contentType)) {
			return contentType;
		}
		return MediaTypeUtils.getMediaForFile (this.extension);
	}

	@Override
	public void pipe (OutputStream os, long position, long count) throws IOException {
		try {
			object.pipe (os, position, count);
		} catch (StorageException e) {
			throw new IOException (e.getMessage (), e);
		}
	}
	
	@Override
	public void pipe (OutputStream os, StreamDecorator decorator, Chunk... chunks) throws IOException {
		try {
			object.pipe (os, decorator, chunks);
		} catch (StorageException e) {
			throw new IOException (e.getMessage (), e);
		}
	}

	@Override
	public Date timestamp () {
		return object.timestamp ();
	}

	@Override
	public InputStream toInput () throws IOException {
		if (object.isFolder ()) {
			return new ByteArrayInputStream (data ().toString ().getBytes ());
		} else {
			try {
				return object.toStreamSource (null, null).stream ();
			} catch (StorageException e) {
				throw new IOException (e.getMessage (), e);
			}
		}
	}

	@Override
	public ApiOutput set (String key, Object value) {
		if (meta == null) {
			meta = new JsonObject ();
		}
		meta.set (key, value);
		return this;
	}

	@Override
	public Object get (String key) {
		if (meta == null) {
			return null;
		}
		return meta.get (key);
	}

	@Override
	public ApiOutput unset (String key) {
		if (meta == null) {
			return this;
		}
		meta.remove (key);
		return this;
	}

}

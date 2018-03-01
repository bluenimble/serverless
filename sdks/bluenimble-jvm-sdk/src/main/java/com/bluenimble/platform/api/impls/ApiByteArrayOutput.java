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
package com.bluenimble.platform.api.impls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

public class ApiByteArrayOutput implements ApiOutput {
	
	private static final long serialVersionUID = 6109917905133029178L;

	private String 		name;
	private byte [] 	bytes;
	private String 		contentType;
	private String 		extension;
	
	private Date 		timestamp = new Date ();

	private JsonObject 	meta;

	public ApiByteArrayOutput (String name, byte [] bytes, String contentType, String extension) {
		this.name 			= name;
		this.bytes 			= bytes;
		this.contentType 	= contentType;
		this.extension 		= extension;
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public Date timestamp () {
		return timestamp;
	}

	@Override
	public String contentType () {
		return contentType;
	}

	@Override
	public String extension () {
		return extension;
	}

	@Override
	public JsonObject data () {
		return (JsonObject)new JsonObject ()
					.set (Defaults.Id, name)
					.set (Defaults.Timestamp, timestamp);
	}

	@Override
	public void pipe (OutputStream out, long position, long count) throws IOException {
		IOUtils.copy (new ByteArrayInputStream (bytes, (int)position, (int)count), out);
	}

	@Override
	public InputStream toInput () throws IOException {
		return new ByteArrayInputStream (bytes);
	}

	@Override
	public long length () {
		return bytes.length;
	}

	@Override
	public JsonObject meta () {
		return meta;
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
	public ApiOutput unset (String key) {
		if (meta == null) {
			return this;
		}
		meta.remove (key);
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
	public void pipe (OutputStream out, StreamDecorator decorator, Chunk... chunks) throws IOException {
		//PIPE
	}

}

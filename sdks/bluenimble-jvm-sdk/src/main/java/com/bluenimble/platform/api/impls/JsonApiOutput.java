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

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.json.AbstractEmitter;
import com.bluenimble.platform.json.JsonEmitter;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

public class JsonApiOutput implements ApiOutput {
	
	private static final long serialVersionUID = 6109917905133029178L;

	private static final String JsonExt = ".json";
	
	interface Spec {
		String Name 		= "name";
		String Timestamp 	= "timestamp";
		String Meta 		= "meta";
		String Data 		= "data";
	}

	private String 		name;
	private Date 		timestamp;

	private JsonObject 	data;
	private JsonObject 	meta;
	
	public JsonApiOutput (JsonObject data, JsonObject meta, String name, Date timestamp) {
		this.data 		= data;
		this.meta 		= meta;
		this.name 		= name;
		this.timestamp 	= timestamp;
	}
	
	public JsonApiOutput (JsonObject data, JsonObject meta, String name) {
		this (data, meta, name, null);
	}

	public JsonApiOutput (JsonObject data, JsonObject meta) {
		this (data, meta, null);
	}
	
	public JsonApiOutput (JsonObject data) {
		this (data, null);
	}
	
	public JsonApiOutput (JsonObject enveloppe, boolean isEnveloppe) {
		if (isEnveloppe) {
			this.name 		= Json.getString (enveloppe, Spec.Name);
			try {
				this.timestamp 	= Lang.toUTC (Json.getString (enveloppe, Spec.Timestamp));
			} catch (Exception e) {
			}
			this.data 		= Json.getObject (enveloppe, Spec.Data);
			this.meta 		= Json.getObject (enveloppe, Spec.Meta);
		} else {
			this.data = enveloppe;
		}
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
		return ApiContentTypes.Json;
	}

	@Override
	public String extension () {
		return JsonExt;
	}

	@Override
	public JsonObject data () {
		return data;
	}

	@Override
	public void pipe (final OutputStream out, long position, long count) throws IOException {
		data ().write (new AbstractEmitter () {
			@Override
			public JsonEmitter write (String chunk) {
				try {
					out.write (chunk.getBytes ());
				} catch (IOException e) {
					throw new RuntimeException (e.getMessage (), e);
				}
				return this;
			}
		});
	}

	@Override
	public void pipe (OutputStream out, StreamDecorator decorator, Chunk... chunks) throws IOException {
		throw new UnsupportedOperationException ("pipe is not supported by JsonApiOutput");
	}

	@Override
	public InputStream toInput () throws IOException {
		if (data == null) {
			return null;
		}
		return new ByteArrayInputStream (data.toString ().getBytes ());
	}

	@Override
	public long length () {
		return -1;
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

}

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
package com.bluenimble.platform.api.impls.scripting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.json.AbstractEmitter;
import com.bluenimble.platform.json.JsonEmitter;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.plugins.scripting.utils.Converters;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

@SuppressWarnings("restriction")
public class ApiSomOutput implements ApiOutput {
	
	private static final long serialVersionUID = 6109917905133029178L;

	private static final String JsonExt = ".json";
	
	interface Spec {
		String Name 		= "name";
		String Timestamp 	= "timestamp";
		String Meta 		= "meta";
		String Data 		= "data";
	}

	private String 				name;
	private Date 				timestamp;

	private ScriptObjectMirror 	data;
	private ScriptObjectMirror 	meta;
	private JsonObject 			altMeta;
	
	public ApiSomOutput (ScriptObjectMirror data, ScriptObjectMirror meta, String name, Date timestamp) {
		this.data 		= data;
		this.meta 		= meta;
		this.name 		= name;
		this.timestamp 	= timestamp;
	}
	
	public ApiSomOutput (ScriptObjectMirror data, ScriptObjectMirror meta, String name) {
		this (data, meta, name, null);
	}

	public ApiSomOutput (ScriptObjectMirror data, ScriptObjectMirror meta) {
		this (data, meta, null);
	}
	
	public ApiSomOutput (ScriptObjectMirror data) {
		this (data, null);
	}
	
	public ApiSomOutput (ScriptObjectMirror enveloppe, boolean isEnveloppe) {
		if (isEnveloppe) {
			this.name 		= (String)enveloppe.getMember (Spec.Name);
			try {
				this.timestamp 	= Lang.toUTC ((String)enveloppe.getMember (Spec.Timestamp));
			} catch (Exception e) {
			}
			this.data 		= (ScriptObjectMirror)enveloppe.getMember (Spec.Data);
			this.meta 		= (ScriptObjectMirror)enveloppe.getMember (Spec.Meta);
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
		return (JsonObject)Converters.convert (data);
	}

	@Override
	public void pipe (final OutputStream out, long position, long length) throws IOException {
		
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
	public void pipe (OutputStream arg0, StreamDecorator arg1, Chunk... arg2) throws IOException {
		throw new UnsupportedOperationException ("pipe chunks is not supported by JsonApiOutput");
	}

	@Override
	public InputStream toInput () throws IOException {
		if (data () == null) {
			return null;
		}
		return new ByteArrayInputStream (data ().toString ().getBytes ());
	}

	@Override
	public long length () {
		return -1;
	}

	@Override
	public JsonObject meta () {
		if (meta == null) {
			return null;
		}
		return (JsonObject)Converters.convert (meta);
	}

	@Override
	public Object get (String key) {
		if (meta == null && altMeta == null) {
			return null;
		}
		return meta != null ? meta.getMember (key) : altMeta.get (key);
	}

	@Override
	public ApiOutput set (String key, Object value) {
		if (meta != null) {
			meta.setMember (key, value);
			return this;
		} 
		
		if (altMeta == null) {
			altMeta = new JsonObject ();
		}
		
		altMeta.set (key, value);
		
		return this;
	}

	@Override
	public ApiOutput unset (String key) {
		if (meta != null) {
			meta.removeMember (key);
			return this;
		} 
		if (altMeta != null) {
			altMeta.remove (key);
		}
		return this;
	}

}

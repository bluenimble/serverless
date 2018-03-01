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
package com.bluenimble.platform.plugins.inbound.http.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable (name = "ApiResponse")
public class HttpApiResponse implements ApiResponse {
	
	private static final long serialVersionUID = -8396891999750710394L;
	
	protected 	HttpServletResponse 	proxy;
	protected 	OutputStream 			out;
	protected 	Writer 					writer;
	
	protected 	Status					status = OK;
	
	protected 	JsonObject 				error;
	
	protected	Map<String, Object>		headers;
	
	private 	JsonObject 				node;
	private 	String 					id;
	
	private 	boolean 				committed;
	
	private 	boolean					headersWritten;
	
	public HttpApiResponse (JsonObject node, String id, HttpServletResponse proxy) throws IOException {
		this.proxy 	= proxy;
		this.node 	= node;
		this.id 	= id;
	}
	
	@Override
	public String getId () {
		return id;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ApiResponse set (String name, Object value) {
		
		if (name == null || value == null) {
			return this;
		}
		
		if (headers == null) {
			headers = new HashMap<String, Object> ();
		}
		
		Object alreadyThere = headers.get (name);
		if (alreadyThere != null) {
			List<Object> values = null;
			if (List.class.isAssignableFrom (alreadyThere.getClass ())) {
				values = (List<Object>)alreadyThere;
			} else {
				values = new ArrayList<Object> ();
				values.add (alreadyThere);
			}
			values.add (value);
			value = values;
		}
		
		headers.put (name, value);
		
		return this;
		
	}

	@Override
	public ApiResponse write (byte [] buff, int offset, int length) throws IOException {
		
		if (committed) {
			return this;
		}
		
		if (buff == null || buff.length == 0) {
			return this;
		}
		
		flushHeaders ();

		boolean written = false;
		if (out != null) {
			out.write (buff, offset, length);
			written = true;
		}
		if (writer != null) {
			writer.write (new String (buff), offset, length);
			written = true;
		}
		if (!written) {
			out = proxy.getOutputStream ();
			out.write (buff, offset, length);
		}
		return this;
	}

	@Override
	public ApiResponse write (Object buff) throws IOException {
		
		if (committed) {
			return this;
		}
		
		if (buff == null) {
			return this;
		}
		
		flushHeaders ();

		boolean written = false;
		if (out != null) {
			out.write (buff.toString ().getBytes ());
			written = true;
		}
		if (writer != null) {
			writer.write (buff.toString ());
			written = true;
		}
		if (!written) {
			out = proxy.getOutputStream ();
			out.write (buff.toString ().getBytes ());
		}
		return this;
	}

	@Override
	public ApiResponse error (Status status, Object message) {
		
		if (this.status == null || (this.status != null && this.status.getCode () < ApiResponse.BAD_REQUEST.getCode ())) {
			this.status = status;
		}
		
		error = new JsonObject ();
		error.set (ApiRequest.Fields.Node.class.getSimpleName ().toLowerCase (), node);
		error.set (ApiRequest.Fields.Id, id);
		error.set (Error.Code, this.status.getCode ());
		
		if (message != null && (message instanceof Object [])) {
			Object [] aMessage = (Object [])message;
			error.set (Error.Message, aMessage [0]);
			error.set (Error.Trace, aMessage [1]);
		} else {
			error.set (Error.Message, message);
		}
		
		return this;
	}
	
	@Override
	public JsonObject getError () {
		return error;
	}

	@Override
	public void close () throws IOException {
		if (out != null) {
			out.flush ();
			out.close ();
		}
		if (writer != null) {
			writer.flush ();
			writer.close ();
		}
		committed = true;
	}

	@Override
	public void reset () {
		proxy.reset ();
	}

	@Override
	public void setStatus (Status status) {
		if (status == null) {
			return;
		}
		this.status = status;
	}

	@Override
	public Status getStatus () {
		return status;
	}

	@Override
	public void setBuffer (int size) {
		proxy.setBufferSize (size);
	}

	@Override
	public OutputStream toOutput () throws IOException {
		if (writer != null) {
			return null;
		}
		if (out == null) {
			out = proxy.getOutputStream ();
		}
		return out;
	}

	@Override
	public Writer toWriter () throws IOException {
		if (out != null) {
			return null;
		}
		if (writer == null) {
			writer = proxy.getWriter ();
		}
		return writer;
	}

	@Override
	public void commit () {
		committed = true;
	}

	@Override
	public boolean isCommitted () {
		return committed;
	}

	@Override
	public void flushHeaders () {
		
		if (headersWritten) {
			return;
		}
		
		if (status != null) {
			proxy.setStatus (status.getCode ());
			status = null;
		}
		
		if (headers == null || headers.isEmpty ()) {
			return;
		}
		Iterator<String> keys = headers.keySet ().iterator ();
		while (keys.hasNext ()) {
			String name = keys.next ();
			
			Object value = headers.get (name);
			if (List.class.isAssignableFrom (value.getClass ())) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>)value;
				for (Object o : list) {
					proxy.addHeader (name, String.valueOf(o));
				}
			} else {
				proxy.setHeader (name, String.valueOf(value));
			}
			
		}
		
		headersWritten = true;
		
	}

}
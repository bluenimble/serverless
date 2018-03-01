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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.json.JsonObject;

public class SimpleApiResponse implements ApiResponse {
	
	private static final long serialVersionUID = 2484293173544350202L;
	
	protected Status						status;
	protected JsonObject 					error;
	
	protected ByteArrayOutputStream			out = new ByteArrayOutputStream ();
	
	protected String 						id;
	
	protected boolean						committed;
	
	protected Map<String, Object>			headers;
	
	public SimpleApiResponse (String id) {
		this.id = id;
	}
	
	@Override
	public void close () throws IOException {
		commit ();
	}

	@Override
	public void commit () {
		committed = true;
	}

	@Override
	public ApiResponse error (Status status, Object message) {
		
		this.status = status;
		
		error = new JsonObject ();
		error.set (RequestID, id);
		error.set (Error.Code, status.getCode ());
		
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
	public void flushHeaders () {
	}

	@Override
	public JsonObject getError () {
		return error;
	}

	@Override
	public String getId () {
		return id;
	}

	@Override
	public boolean isCommitted () {
		return false;
	}

	@Override
	public void reset () {
	}

	@Override
	public ApiResponse set (String name, Object value) {
		if (headers == null) {
			headers = new HashMap<String, Object> ();
		}
		headers.put (name, value);
		return this;
	}

	@Override
	public void setBuffer (int size) {
	}

	@Override
	public Status getStatus () {
		return status;
	}
	@Override
	public void setStatus (Status status) {
		this.status = status;
	}
	
	@Override
	public OutputStream toOutput () throws IOException {
		return out;
	}

	@Override
	public Writer toWriter () throws IOException {
		return new OutputStreamWriter (out);
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

		out.write (buff, offset, length);

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

		out.write (buff.toString ().getBytes ());

		return this;
	}
	
	public byte [] toBytes () {
		byte [] bytes = out.toByteArray ();
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		return bytes;
	}

}

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.json.JsonObject;

public abstract class AbstractApiResponse implements ApiResponse {
	
	private static final long serialVersionUID = 2484293173544350202L;
	
	protected Status						status;
	protected JsonObject 					error;
	
	protected String 						id;
	
	protected boolean						committed;
	
	protected Map<String, Object>			headers;
	
	protected AbstractApiResponse (String id) {
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
	public ApiResponse write (byte [] buff, int offset, int length) throws IOException {
		
		if (committed) {
			return this;
		}
		
		if (buff == null || buff.length == 0) {
			return this;
		}
		
		flushHeaders ();

		return append (buff, offset, length);
	}

	@Override
	public ApiResponse write (Object buff) throws IOException {
		
		if (buff == null) {
			return this;
		}
		
		byte [] bytes = buff.toString ().getBytes ();

		return write (bytes, 0, bytes.length);
	}
	
	protected abstract ApiResponse append (byte [] buff, int offset, int length) throws IOException;

}

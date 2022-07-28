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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.json.JsonObject;

public abstract class AbstractApiResponse extends BaseApiResponse {
	
	private static final long serialVersionUID = 2484293173544350202L;
	
	private static final Set<String> Unchangeable = new HashSet<String> ();
	static {
		Unchangeable.add (ApiHeaders.ContentType);
	}
	
	protected Status						status = OK;
	protected JsonObject 					error;
	
	protected boolean						committed;
	
	protected Map<String, Object>			headers;
	
	protected AbstractApiResponse (String id, JsonObject node) {
		super (id, node);
	}
	
	@Override
	public ApiResponse error (Status status, Object message) {
		
		if (this.status == null || (this.status != null && this.status.getCode () < ApiResponse.BAD_REQUEST.getCode ())) {
			this.status = status;
		}
		
		error = new JsonObject ();
		error.set (ApiRequest.Fields.Node.class.getSimpleName ().toLowerCase (), node);
		error.set (ApiRequest.Fields.Id, id);
		if (service != null) {
			error.set (ApiRequest.Fields.Service, service.getId ());
		}
		error.set (Error.Code, this.status.getCode ());
		
		// no message
		if (message == null) {
			return this;
		}
		
		// a message, trace, properties array
		if (message instanceof Object []) {
			Object [] aMessage = (Object [])message;
			error.set (Error.Message, aMessage [0]);
			error.set (Error.Trace, aMessage [1]);
			if (aMessage [2] != null) {
				error.merge ((JsonObject)aMessage [2]);
			}
			return this;
		}
		
		// a string or object message
		error.set (Error.Message, message);
		return this;
	}
	
	@Override
	public JsonObject getError () {
		return error;
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
	public boolean isCommitted () {
		return false;
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
		
		if (alreadyThere != null && Unchangeable.contains (name)) {
			return this;
		}
		
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

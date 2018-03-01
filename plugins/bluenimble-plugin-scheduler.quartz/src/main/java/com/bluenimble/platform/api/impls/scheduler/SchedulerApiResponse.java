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
package com.bluenimble.platform.api.impls.scheduler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable (name = "ApiResponse")
public class SchedulerApiResponse implements ApiResponse {

	private static final long serialVersionUID = -5269972265851329885L;
	
	protected Status						status;
	protected JsonObject 					error;
	protected ApiServiceExecutionException 	exception;
	
	protected String 						id;
	
	public SchedulerApiResponse (String id) {
		this.id = id;
	}
	
	@Override
	public void close () throws IOException {
	}

	@Override
	public void commit () {
	}

	@Override
	public ApiResponse error (Status status, Object message) {
		
		this.status = status;
		
		error = new JsonObject ();
		error.set (RequestID, id);
		error.set (Error.Code, status.getCode ());
		if (message != null && (message instanceof String [])) {
			String [] aMessage = (String [])message;
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
		return null;
	}

	@Override
	public Writer toWriter () throws IOException {
		return null;
	}

	@Override
	public ApiResponse write (Object content) throws IOException {
		return this;
	}

	@Override
	public ApiResponse write (byte[] content, int offset, int len)
			throws IOException {
		return this;
	}

	public void setException (ApiServiceExecutionException exception) {
		this.exception = exception;
	}
	public ApiServiceExecutionException getException () {
		return exception;
	}
	
}

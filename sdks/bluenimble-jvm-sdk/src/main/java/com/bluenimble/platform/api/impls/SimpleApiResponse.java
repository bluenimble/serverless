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

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.json.JsonObject;

public class SimpleApiResponse extends AbstractApiResponse {
	
	private static final long serialVersionUID = 2484293173544350202L;
	
	protected OutputStream out;
	
	public SimpleApiResponse (String id, JsonObject node, OutputStream out) {
		super (id, node);
		if (out == null) {
			out = new ByteArrayOutputStream ();
		}
		this.out = out;
	}
	
	public SimpleApiResponse (String id, JsonObject node) {
		this (id, node, null);
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
	public ApiResponse append (byte [] buff, int offset, int length) throws IOException {
		
		out.write (buff, offset, length);

		return this;
	}
	
	public byte [] toBytes () {
		
		if (out instanceof ByteArrayOutputStream) {
			throw new UnsupportedOperationException ("Operation toBytes isn't supported by " + this.getClass ().getSimpleName ());
		}
		
		byte [] bytes = ((ByteArrayOutputStream)out).toByteArray ();
				
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		
		return bytes;
	}

	@Override
	public void setBuffer (int size) {
	}

	@Override
	public void reset () {
	}

	@Override
	public void flushHeaders () {
	}

}

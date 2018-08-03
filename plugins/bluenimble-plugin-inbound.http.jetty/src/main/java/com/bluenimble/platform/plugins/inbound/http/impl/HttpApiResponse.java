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
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.impls.AbstractApiResponse;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable (name = "ApiResponse")
public class HttpApiResponse extends AbstractApiResponse {
	
	private static final long serialVersionUID = -8396891999750710394L;
	
	protected 	HttpServletResponse 	proxy;
	protected 	OutputStream 			out;
	protected 	Writer 					writer;
	
	private 	boolean					headersWritten;
	
	public HttpApiResponse (JsonObject node, String id, HttpServletResponse proxy) throws IOException {
		super (id, node);
		this.proxy 	= proxy;
	}
	
	@Override
	public ApiResponse append (byte [] buff, int offset, int length) throws IOException {
		
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
	public void close () throws IOException {
		super.close ();
		if (out != null) {
			out.flush ();
			out.close ();
		}
		if (writer != null) {
			writer.flush ();
			writer.close ();
		}
	}

	@Override
	public void reset () {
		proxy.reset ();
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

	public HttpServletResponse getProxy () {
		return proxy;
	}

}
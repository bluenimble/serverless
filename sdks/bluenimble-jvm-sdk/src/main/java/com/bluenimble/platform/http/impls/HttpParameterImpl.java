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
package com.bluenimble.platform.http.impls;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.bluenimble.platform.http.HttpParameter;
import com.bluenimble.platform.http.utils.HttpUtils;

public class HttpParameterImpl implements HttpParameter {
	
	private static final long serialVersionUID = -2340728072298016526L;
	
	protected String name;
	protected Object value;
	
	public HttpParameterImpl (String name, Object value) {
		this.name = name;
		this.value = value;
	}
	
	@Override
	public String getName () {
		return name;
	}
	
	public void setName (String name) {
		this.name = name;
	}
	
	@Override
	public Object getValue () {
		return value;
	}
	
	public void setValue (Object value) {
		this.value = value;
	}
	
	@Override
	public String dump (StringBuilder sb, String charset) throws UnsupportedEncodingException {
		if (charset == null) {
			charset = HttpUtils.DEFAULT_ENCODING;
		}
		boolean returnResult = false;
		if (sb == null) {
			returnResult = true;
			sb = new StringBuilder ();
		}
		if (name != null) {
			sb.append (URLEncoder.encode (name, charset));
		}
		if (value != null) {
			sb.append ("=");
			sb.append (URLEncoder.encode (value.toString (), charset));
		}
		if (returnResult) {
			String s = sb.toString ();
			sb.setLength (0);
			sb = null;
			return s;
		}
		return null;
	}
	
	public String toString () {
		StringBuilder sb = new StringBuilder ();
		try {
			dump (sb, HttpUtils.DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			// IGNORE
		}
		String s = sb.toString ();
		sb.setLength (0);
		sb = null;
		return s;
	}
	
}

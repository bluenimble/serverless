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

import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.http.HttpHeader;

public class HttpHeaderImpl implements HttpHeader {
	
	private static final long serialVersionUID = -2340728072298016526L;
	
	private static final String [] EMPTY_ARRAY = new String [] { };
	
	protected String name;
	protected List<String> values;
	
	public HttpHeaderImpl (String name, String value) {
		this.name = name;
		if (value != null) {
			this.values = new ArrayList<String> ();
			this.values.add (value);
		}
	}
	
	public HttpHeaderImpl (String name, List<String> values) {
		this.name = name;
		this.values = values;
	}
	
	@Override
	public String getName () {
		return name;
	}
	
	public void setName (String name) {
		this.name = name;
	}
	
	@Override
	public String [] getValues () {
		if (values == null || values.isEmpty ()) {
			return EMPTY_ARRAY;
		} 
		return values.toArray (new String [values.size ()]);
	}
	
	public String toString () {
		StringBuilder sb = new StringBuilder ();
		if (name != null) {
			sb.append ("[").append (name).append ("]").append ("=>");
		}
		if (values != null && !values.isEmpty ()) {
			for (int i = 0; i < values.size (); i++) {
				Object v  = values.get (i);
				sb.append (v);
				if (i < (values.size () - 1)) {
					sb.append ("|");
				}
			}
		}
		String s = sb.toString ();
		sb.setLength (0);
		sb = null;
		return s;
	}
	
}

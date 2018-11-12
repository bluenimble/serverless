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

import java.io.InputStream;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.api.ApiStreamSource;

public class DefaultApiStreamSource implements ApiStreamSource {

	private static final long serialVersionUID = -6134071700402601809L;
	
	private String 		id;
	private String 		name;
	private String 		contentType;
	private InputStream stream;
	private boolean		closable;
	private long 		length;
	
	public DefaultApiStreamSource (String id, String name, String contentType, long length, InputStream stream) {
		this.id 			= id;
		this.name 			= name;
		this.contentType 	= contentType;
		this.length 		= length;
		this.stream 		= stream;
	}
	
	@Override
	public String id () {
		return id;
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public String contentType () {
		return contentType;
	}

	@Override
	public InputStream stream () {
		return stream;
	}
	
	@Override
	public long length () {
		return length;
	}

	@Override
	public void close () {
		if (!closable) {
			return;
		}
		IOUtils.closeQuietly (stream);
	}
	
	public DefaultApiStreamSource setClosable (boolean closable) {
		this.closable = closable;
		return this;
	}

}

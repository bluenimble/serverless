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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.bluenimble.platform.IOUtils;

public class InputStreamHttpMessageBodyPart extends AbstractHttpMessageBodyPart {
	
	private static final long serialVersionUID = 5209163648562691869L;

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	protected InputStream 	content;
	protected String 		fileName;

	public InputStreamHttpMessageBodyPart (InputStream content) {
		this.content = content;
	}
	
	public InputStreamHttpMessageBodyPart (String name, String fileName, InputStream content) {
		super (name);
		this.fileName = fileName;
		this.content = content;
	}
	
	@Override
	public void dump (OutputStream output, String charset) throws IOException {
		if (content == null) {
			return;
		}
		try {
			byte [] buffer = new byte [DEFAULT_BUFFER_SIZE];
			int n = 0;
			while (-1 != (n = content.read (buffer))) {
				output.write (buffer, 0, n);
			}
			output.flush ();
		} finally {
			IOUtils.closeQuietly (content);
		}
	}

	@Override
	public void close () {
		if (content == null) {
			return;
		}
		try {
			content.close ();
		} catch (IOException ioex) {
			// IGNORE
		}
	}
	
	@Override
	public InputStream toInputStream () {
		return content;
	}

	@Override
	public String getFileName () {
		if (fileName != null) {
			return fileName;
		}
		return getName ();
	}

}

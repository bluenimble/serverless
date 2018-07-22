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
package com.bluenimble.platform.storage.impls;

import java.io.IOException;
import java.io.OutputStream;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Recyclable;

public class RecyclableOutputStream extends OutputStream implements Recyclable {

	private static final long serialVersionUID = 7867377376863560794L;
	
	private OutputStream proxy;
	
	public RecyclableOutputStream (OutputStream proxy) {
		this.proxy = proxy;
	}

	@Override
	public void write (int b) throws IOException {
		proxy.write (b);
	}

	@Override
	public void write (byte [] b) throws IOException {
		proxy.write (b);
	}

	@Override
	public void write (byte [] b, int off, int len) throws IOException {
		proxy.write (b, off, len);
	}

	@Override
	public void flush () throws IOException {
		proxy.flush ();
	}

	@Override
	public void close () throws IOException {
		IOUtils.closeQuietly (proxy);
	}

	@Override
	public void recycle () {
		IOUtils.closeQuietly (proxy);
	}
	
}

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
import java.io.InputStream;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Recyclable;

public class RecyclableInputStream extends InputStream implements Recyclable {

	private static final long serialVersionUID = 7867377376863560794L;
	
	private InputStream proxy;
	
	public RecyclableInputStream (InputStream proxy) {
		this.proxy = proxy;
	}

	@Override
	public int read () throws IOException {
		return proxy.read ();
	}

	@Override
	public int read (byte [] b) throws IOException {
		return proxy.read (b);
	}

	@Override
	public int read (byte [] b, int off, int len) throws IOException {
		return proxy.read (b, off, len);
	}

	@Override
	public long skip (long n) throws IOException {
		return proxy.skip (n);
	}

	@Override
	public int available () throws IOException {
		return proxy.available ();
	}

	@Override
	public void close () throws IOException {
		IOUtils.closeQuietly (proxy);
	}

	@Override
	public synchronized void mark (int readlimit) {
		proxy.mark (readlimit);
	}

	@Override
	public synchronized void reset () throws IOException {
		proxy.reset ();
	}

	@Override
	public boolean markSupported () {
		return proxy.markSupported ();
	}

	@Override
	public void finish (boolean withError) {
	}

	@Override
	public void recycle () {
		IOUtils.closeQuietly (proxy);
	}
	
}

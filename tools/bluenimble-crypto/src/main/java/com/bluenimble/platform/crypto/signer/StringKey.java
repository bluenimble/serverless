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
package com.bluenimble.platform.crypto.signer;

import java.io.UnsupportedEncodingException;
import java.security.Key;

import com.bluenimble.platform.crypto.Algorithm;

public class StringKey implements Key {
	
	private static final long serialVersionUID = 1007262587318229026L;

	private 			Algorithm algorithm = Algorithm.HmacSHA1;
	private transient 	byte [] key;
	
	public StringKey (String skey, String encoding, Algorithm algorithm) throws UnsupportedEncodingException {
		if (encoding == null) {
			encoding = "UTF-8";
		}
		key = skey.getBytes (encoding);
		if (algorithm == null) {
			algorithm = Algorithm.HmacSHA1;
		}
		this.algorithm = algorithm;
	}
	
	public StringKey (String skey, String encoding) throws UnsupportedEncodingException {
		this (skey, encoding, null);
	}
	
	public StringKey (String skey, Algorithm algorithm) throws UnsupportedEncodingException {
		this (skey, null, algorithm);
	}
	
	public StringKey (String skey) throws UnsupportedEncodingException {
		this (skey, (String)null);
	}
	
	public StringKey (byte [] key) throws UnsupportedEncodingException {
		this.key = key;
	}
	
	@Override
	public String getAlgorithm () {
		if (algorithm == null) {
			return Algorithm.HmacSHA1.getId ();
		}
		return algorithm.getId ();
	}

	@Override
	public byte [] getEncoded () {
		return key;
	}

	@Override
	public String getFormat () {
		throw new UnsupportedOperationException ("StringKey.getFormat not supported");
	}

}

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
package com.bluenimble.platform.crypto.signer.impl;

import java.security.Key;

import com.bluenimble.platform.crypto.SignatureAware;

public class DefaultSignatureAwareSecureDocument 
	extends DefaultSecureDocument implements SignatureAware {

	private static final long serialVersionUID = -30758975311218655L;
	
	private transient Key key;
	private transient byte [] signature;
	
	public DefaultSignatureAwareSecureDocument (byte [] bytes, Key key) {
		super (bytes);
		this.key = key;
	}

	public DefaultSignatureAwareSecureDocument (byte [] bytes) {
		super (bytes);
	}
	
	public DefaultSignatureAwareSecureDocument () {
		super ();
	}

	@Override
	public Key getKey () {
		return key;
	}

	@Override
	public void setKey (Key key) {
		this.key = key;
	}

	@Override
	public byte [] getSignature () {
		return signature;
	}

	@Override
	public void setSignature (byte [] signature) {
		this.signature = signature;
	}

}

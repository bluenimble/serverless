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

import com.bluenimble.platform.crypto.SecureDocument;

public class DefaultSecureDocument implements SecureDocument {

	private static final long serialVersionUID = -30758975311218655L;

	protected byte [] bytes;
	
	public DefaultSecureDocument (){
	}
	
	public DefaultSecureDocument (byte [] bytes) {
		this.bytes = bytes;
	}
	
	@Override
	public byte [] getBytes () {
		return bytes;
	}

	@Override
	public void setBytes (byte [] bytes) {
		this.bytes = bytes;
	}

}

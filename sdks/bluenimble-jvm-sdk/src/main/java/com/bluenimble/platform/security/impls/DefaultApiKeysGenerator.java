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
package com.bluenimble.platform.security.impls;

import javax.crypto.KeyGenerator;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.security.ApiKeysGenerator;
import com.bluenimble.platform.api.security.ApiKeysGeneratorException;
import com.bluenimble.platform.encoding.Base64;

public class DefaultApiKeysGenerator implements ApiKeysGenerator {

	private static final long serialVersionUID = -5113876047087704787L;
	
	protected String algorithm;
	protected String encoding;
	
	@Override
	public String [] generate () throws ApiKeysGeneratorException {
		
		if (Lang.isNullOrEmpty (algorithm)) {
			algorithm = "HMACSHA1";
		}
		
		if (Lang.isNullOrEmpty (encoding)) {
			encoding = Encodings.UTF8;
		}
		
		try {
			KeyGenerator generator = KeyGenerator.getInstance (algorithm);

			generator.init (120);
			byte[] accessKey = generator.generateKey ().getEncoded ();

			generator.init (240);
			byte[] secretKey = generator.generateKey ().getEncoded ();

			return new String [] {hash (accessKey, encoding), hash (secretKey, encoding)};
		} catch (Exception ex) {
			throw new ApiKeysGeneratorException (ex);
		}
		
	}

	public void setAlgorithm (String algorithm) {
		this.algorithm = algorithm;
	}
	public String getAlgorithm () {
		return algorithm;
	}

	public void setEncoding (String encoding) {
		this.encoding = encoding;
	}
	public String getEncoding () {
		return encoding;
	}

	private static String hash (byte [] key, String encoding) throws Exception {
		return new String (Base64.encodeBase64 (key), encoding).trim ();
	}
	
	public static void main(String[] args) throws ApiKeysGeneratorException {
		String [] keys = new DefaultApiKeysGenerator ().generate ();
		System.out.println (keys [0]);
		System.out.println (keys [1]);
	}
	
}

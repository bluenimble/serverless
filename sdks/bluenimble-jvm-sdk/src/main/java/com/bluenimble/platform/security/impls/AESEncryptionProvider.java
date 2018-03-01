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

import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.security.EncryptionProvider;
import com.bluenimble.platform.security.EncryptionProviderException;

public class AESEncryptionProvider implements EncryptionProvider {

	private static final long serialVersionUID = -5350030617735769829L;
	
	protected String provider;
	
	public AESEncryptionProvider () {
		this (null);
	}

	public AESEncryptionProvider (String provider) {
		if (provider == null) {
			provider = "SunJCE";
		}
		this.provider = provider;
	}

	@Override
	public void crypt (InputStream is, OutputStream os, String key, Mode mode) throws EncryptionProviderException {
		
		int m = Cipher.ENCRYPT_MODE;
		
		switch (mode) {
			case Encrypt:
				m = Cipher.ENCRYPT_MODE;
				break;
			case Decrypt:
				m = Cipher.DECRYPT_MODE;
				break;
			default:
				break;
		}
		
		CipherOutputStream cos = null;
		
		try {
			SecretKeySpec secretKey = new SecretKeySpec (key.getBytes (),"AES");
			
			Cipher cipher = Cipher.getInstance ("AES", provider);

			cipher.init (m, secretKey);

			if (m == Cipher.ENCRYPT_MODE) {
				CipherInputStream cis = new CipherInputStream (is, cipher);
				IOUtils.copy (cis, os);
			} else if (m == Cipher.DECRYPT_MODE) {
				cos = new CipherOutputStream (os, cipher);
				IOUtils.copy (is, cos);
			}
		} catch (Exception e) {
			throw new EncryptionProviderException (e.getMessage (), e);
		} finally {
			IOUtils.closeQuietly (cos);
		}
		
	}

}

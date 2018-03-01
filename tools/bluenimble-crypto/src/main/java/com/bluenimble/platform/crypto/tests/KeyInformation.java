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
package com.bluenimble.platform.crypto.tests;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class KeyInformation {
	private PrivateKey privateKey;
	private X509Certificate certificate;
	private PublicKey publicKey;
	
	public KeyInformation (PrivateKey privateKey, X509Certificate certificate, PublicKey publicKey) {
		this.privateKey = privateKey;
		this.certificate = certificate;
		this.publicKey = publicKey;
	}
	
	public String toString () {
		StringBuilder sb = new StringBuilder ();
		if (this.publicKey != null) {
			sb.append ("Public Key\n").append(this.publicKey.toString ()).append ("\n");
		}
		if (this.privateKey != null) {
			sb.append ("Private Key\n").append(this.privateKey.toString ()).append ("\n");
		}
		if (this.certificate != null) {
			sb.append ("Certificate\n").append(this.certificate.toString ()).append ("\n");
		}
		String str = sb.toString ();
		sb.setLength (0);
		sb = null;
		return str;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}
	
}

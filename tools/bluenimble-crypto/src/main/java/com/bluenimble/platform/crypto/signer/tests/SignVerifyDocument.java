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
package com.bluenimble.platform.crypto.signer.tests;


import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.crypto.SecureDocument;
import com.bluenimble.platform.crypto.generator.CertificatesManager;
import com.bluenimble.platform.crypto.generator.StoreLoaderException;
import com.bluenimble.platform.crypto.generator.impl.DefaultCertificatesManager;
import com.bluenimble.platform.crypto.signer.CertificateAcceptor;
import com.bluenimble.platform.crypto.signer.Signer;
import com.bluenimble.platform.crypto.signer.SignerException;
import com.bluenimble.platform.crypto.signer.impl.DefaultSigner;
import com.bluenimble.platform.crypto.signer.impl.StringSecureDocument;
import com.bluenimble.platform.crypto.tests.ReadX509;

public class SignVerifyDocument {
	
	public static void main (String[] args) throws StoreLoaderException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, SignerException, IOException {
		
		String password = "beesphere";
		String alias = "beesphere";
		String p12 = "beesphere.p12";
		final String cer = "beesphere.cer";
		
		CertificatesManager cm = new DefaultCertificatesManager ();
		Map<String, Object> properties = new HashMap<String, Object> ();
		properties.put (CertificatesManager.KEY_PASSWORD, password);
		KeyStore ks = cm.load (new FileInputStream(p12), properties);
		PrivateKey key = (PrivateKey) ks.getKey (alias, password.toCharArray ());
		
		Signer signer = new DefaultSigner ();
		
		SecureDocument doc = new StringSecureDocument ("a document to sign");
		signer.sign (
			doc, 
			key, 
			new X509Certificate [] {ReadX509.read (new FileInputStream(cer))}
		);
		System.out.println (new String (doc.getBytes ()));
		
		signer.verify (doc, new CertificateAcceptor () {
			private static final long serialVersionUID = 8524753501741582177L;
			@Override
			public boolean accept (X509Certificate cert) throws SignerException {
				try {
					return cert.equals (ReadX509.read (new FileInputStream (cer)));
				} catch (Throwable th) {
					throw new SignerException (th, th.getMessage ());
				} 
			}
		});
		
		System.out.println (new String (doc.getBytes ()));

	}
	
}
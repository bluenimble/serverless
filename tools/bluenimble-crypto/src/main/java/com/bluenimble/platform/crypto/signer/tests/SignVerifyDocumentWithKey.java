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


import java.io.IOException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.bluenimble.platform.crypto.Algorithm;
import com.bluenimble.platform.crypto.generator.StoreLoaderException;
import com.bluenimble.platform.crypto.signer.Signer;
import com.bluenimble.platform.crypto.signer.SignerException;
import com.bluenimble.platform.crypto.signer.StringKey;
import com.bluenimble.platform.crypto.signer.impl.DefaultSigner;
import com.bluenimble.platform.crypto.signer.impl.SimpleSigner;
import com.bluenimble.platform.crypto.signer.impl.StringSignatureAwareSecureDocument;

public class SignVerifyDocumentWithKey {
	
	public static void main (String[] args) throws StoreLoaderException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, SignerException, IOException {
		
		Signer signer = new SimpleSigner ();
		
		Key key = new StringKey ("mykey", Algorithm.HmacSHA1);
		
		StringSignatureAwareSecureDocument doc = new StringSignatureAwareSecureDocument ("a document to sign");
		doc.setKey (key);
		
		signer.sign (
			doc, 
			key, 
			null
		);
		System.out.println (new String (doc.getSignature (), DefaultSigner.BASE64_ENCODING));
		
		signer.verify (doc, null);
		
		System.out.println (new String (doc.getBytes ()));

	}
	
}

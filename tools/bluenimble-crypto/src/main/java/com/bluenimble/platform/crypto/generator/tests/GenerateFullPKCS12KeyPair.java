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
package com.bluenimble.platform.crypto.generator.tests;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.crypto.generator.CertificatesManager;
import com.bluenimble.platform.crypto.generator.CertificatesManagerException;
import com.bluenimble.platform.crypto.generator.impl.DefaultCertificatesManager;
import com.bluenimble.platform.crypto.tests.ReadPKCS12;

public class GenerateFullPKCS12KeyPair {

	public static void main (String[] args) throws CertificatesManagerException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		String fileName = "files/testFull.p12";
		
		// Create generator instance
		CertificatesManager generator = new DefaultCertificatesManager ();
		
		// Properties
		Map<String, Object> props = new HashMap<String, Object> ();
		props.put (CertificatesManager.COMMON_NAME, "bs");
		props.put (CertificatesManager.ORGANIZATION_NAME, "bs");
		props.put (CertificatesManager.ORGANIZATION_UNIT, "bs");
		props.put (CertificatesManager.COUNTRY, "MA");
		props.put (CertificatesManager.STATE, "MA");
		props.put (CertificatesManager.LOCALITY_NAME, "Rabat");
		props.put (CertificatesManager.STORE, "PKCS12");
		props.put (CertificatesManager.KEY_SIZE, 1024);
		props.put (CertificatesManager.KEY_ALGORITHM, "RSA");
		props.put (CertificatesManager.SIGNATURE_ALGORITHM, "MD5withRSA");
		props.put (CertificatesManager.EMAIL, "ait@beesphere.com");
		props.put (CertificatesManager.KEY_PASSWORD, "beesphere");
		
		// Generate keypair
		OutputStream os = new FileOutputStream (fileName);
		generator.generate (props, os);
		os.close ();
		
		// Read keypair
		
		System.out.println (
			ReadPKCS12.read (new FileInputStream (fileName), 
					"beesphere", DefaultCertificatesManager.DEFAULT_ALIAS)
		);
		
		
	}

}


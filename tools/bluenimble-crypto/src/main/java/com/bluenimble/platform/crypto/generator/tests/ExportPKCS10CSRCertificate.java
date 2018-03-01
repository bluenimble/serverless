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
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.crypto.generator.CertificateExportException;
import com.bluenimble.platform.crypto.generator.CertificatesManager;
import com.bluenimble.platform.crypto.generator.StoreLoaderException;
import com.bluenimble.platform.crypto.generator.impl.DefaultCertificatesManager;

public class ExportPKCS10CSRCertificate {

	public static void main (String[] args) throws CertificateExportException, StoreLoaderException, IOException, CertificateException {
		String storeFileName = "files/test.p12";
		String certFileName = "files/test.csr";
		
		Map<String, Object> EMPTY_MAP = new HashMap<String, Object> ();
		
		// Create generator instance
		CertificatesManager manager = new DefaultCertificatesManager ();
		
		// Export Certification Request
		OutputStream os = new FileOutputStream (certFileName);
		manager.export (
			manager.load (new FileInputStream (storeFileName), EMPTY_MAP), 
			EMPTY_MAP, 
			CertificatesManager.ExportFormat.PKCS10_CSR, 
			os
		);
		os.close ();
		
	}

}

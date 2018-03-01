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
package com.bluenimble.platform.crypto.generator;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.KeyStore;
import java.util.Map;

public interface CertificatesManager extends Serializable {
	
	String PACKAGE = CertificatesManager.class.getPackage().getName ();

	String KEY_ALGORITHM 		= PACKAGE + ".keyAlgorithm";
	String KEY_SIZE 			= PACKAGE + ".keySize";

	String SIGNATURE_ALGORITHM 	= PACKAGE + ".signatureAlgorithm";
	String VALIDITY				= PACKAGE + ".validity";
	String COMMON_NAME 			= PACKAGE + ".commonName";
	String ORGANIZATION_UNIT 	= PACKAGE + ".organizationUnit";
	String ORGANIZATION_NAME 	= PACKAGE + ".organizationName";
	String LOCALITY_NAME 		= PACKAGE + ".localityName";
	String STATE 				= PACKAGE + ".state";
	String COUNTRY 				= PACKAGE + ".country";
	String EMAIL 				= PACKAGE + ".email";

	String STORE 				= PACKAGE + ".store";
	
	String KEY_PASSWORD			= PACKAGE + ".keyPassword";
	String KEY_ALIAS			= PACKAGE + ".keyAlias";
	
	String EXPORT_ALL			= PACKAGE + ".exportAll";
	
	enum ExportFormat {
		DER,
		PKCS7,
		PKI_PATH,
		PEM,
		PKCS10_CSR
	}
	
	void generate (Map<String, Object> properties, OutputStream os) 
			throws CertificatesManagerException; 
	
	KeyStore load (InputStream is, Map<String, Object> properties) throws StoreLoaderException;
	
	void export   (KeyStore keyStore, Map<String, Object> properties, ExportFormat exportFormat, OutputStream os) 
			throws CertificateExportException; 
	
}

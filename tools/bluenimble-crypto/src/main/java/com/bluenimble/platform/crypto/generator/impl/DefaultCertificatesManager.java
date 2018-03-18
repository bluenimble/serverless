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
package com.bluenimble.platform.crypto.generator.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import com.bluenimble.platform.crypto.generator.CertificateExportException;
import com.bluenimble.platform.crypto.generator.CertificatesManager;
import com.bluenimble.platform.crypto.generator.CertificatesManagerException;
import com.bluenimble.platform.crypto.generator.StoreLoaderException;

import net.sf.portecle.crypto.CryptoException;
import net.sf.portecle.crypto.KeyPairType;
import net.sf.portecle.crypto.KeyPairUtil;
import net.sf.portecle.crypto.KeyStoreType;
import net.sf.portecle.crypto.KeyStoreUtil;
import net.sf.portecle.crypto.SignatureType;
import net.sf.portecle.crypto.X509CertUtil;

public class DefaultCertificatesManager implements CertificatesManager {

	private static final long serialVersionUID = 7235899384894169854L;

	static {
		Security.addProvider (new BouncyCastleProvider());
	}

	public static final String X509 = "X.509";
	private static final String PKCS7_ENCODING = "PKCS7";
	private static final String PKIPATH_ENCODING = "PkiPath";

	private static final String DUMMY_NAME = "test";
	private static final String DUMMY_EMAIL = "test@test.com";
	public static final String DUMMY_PASS = "bspass";

	public static final String DEFAULT_ALIAS = "bscert";

	private static final Map<String, KeyStoreType> STORE_TYPES = new HashMap<String, KeyStoreType>();
	
	static {
		STORE_TYPES.put(KeyStoreType.BKS.name (), KeyStoreType.BKS);
		STORE_TYPES.put(KeyStoreType.CaseExactJKS.name (), KeyStoreType.CaseExactJKS);
		STORE_TYPES.put(KeyStoreType.GKR.name (), KeyStoreType.GKR);
		STORE_TYPES.put(KeyStoreType.JCEKS.name (), KeyStoreType.JCEKS);
		STORE_TYPES.put(KeyStoreType.JKS.name (), KeyStoreType.JKS);
		STORE_TYPES.put(KeyStoreType.PKCS11.name (), KeyStoreType.PKCS11);
		STORE_TYPES.put(KeyStoreType.PKCS12.name (), KeyStoreType.PKCS12);
		STORE_TYPES.put(KeyStoreType.UBER.name (), KeyStoreType.UBER);
	}

	private static final Map<String, KeyPairType> KEY_ALGORITHMS = new HashMap<String, KeyPairType>();
	static {
		KEY_ALGORITHMS.put(KeyPairType.RSA.name(), KeyPairType.RSA);
		KEY_ALGORITHMS.put(KeyPairType.DSA.name(), KeyPairType.DSA);
		KEY_ALGORITHMS.put(KeyPairType.ECDSA.name(), KeyPairType.ECDSA);
	}

	private static final Map<String, SignatureType> SIGN_ALGORITHMS = new HashMap<String, SignatureType>();
	static {
		SIGN_ALGORITHMS.put(SignatureType.MD2withRSA.name(),
				SignatureType.MD2withRSA);
		SIGN_ALGORITHMS.put(SignatureType.MD5withRSA.name(),
				SignatureType.MD5withRSA);
		SIGN_ALGORITHMS.put(SignatureType.RIPEMD128withRSA.name(),
				SignatureType.RIPEMD128withRSA);
		SIGN_ALGORITHMS.put(SignatureType.RIPEMD160withRSA.name(),
				SignatureType.RIPEMD160withRSA);
		SIGN_ALGORITHMS.put(SignatureType.RIPEMD256withRSA.name(),
				SignatureType.RIPEMD256withRSA);
		SIGN_ALGORITHMS.put(SignatureType.SHA1withDSA.name(),
				SignatureType.SHA1withDSA);
		SIGN_ALGORITHMS.put(SignatureType.SHA1withECDSA.name(),
				SignatureType.SHA1withECDSA);
		SIGN_ALGORITHMS.put(SignatureType.SHA1withRSA.name(),
				SignatureType.SHA1withRSA);
		SIGN_ALGORITHMS.put(SignatureType.SHA224withRSA.name(),
				SignatureType.SHA224withRSA);
		SIGN_ALGORITHMS.put(SignatureType.SHA256withRSA.name(),
				SignatureType.SHA256withRSA);
		SIGN_ALGORITHMS.put(SignatureType.SHA384withRSA.name(),
				SignatureType.SHA384withRSA);
		SIGN_ALGORITHMS.put(SignatureType.SHA512withRSA.name(),
				SignatureType.SHA512withRSA);
	}

	private Object getProperty(Map<String, Object> properties, String name,
			Object defaultValue) {
		if (name == null) {
			return null;
		}
		Object value = properties.get(name);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	private String getStringProperty(Map<String, Object> properties,
			String name, String defaultValue) {
		Object value = getProperty(properties, name, defaultValue);
		if (value != null && value instanceof String) {
			System.out.println("name : " + name + " / value : " + value);
			return (String) value;
		}
		System.out.println("Default name : " + name + " / value : " + value);
		return null;
	}

	private int getIntProperty(Map<String, Object> properties, String name,
			int defaultValue) {
		Object value = getProperty(properties, name, defaultValue);
		if (value != null && value instanceof Integer) {
			return (Integer) value;
		}
		return defaultValue;
	}

	private boolean getBooleanProperty(Map<String, Object> properties, String name,
			boolean defaultValue) {
		Object value = getProperty (properties, name, defaultValue);
		if (value != null && value instanceof Boolean) {
			return (Boolean) value;
		}
		return false;
	}

	@Override
	public void generate(Map<String, Object> properties, OutputStream os)
			throws CertificatesManagerException {
		KeyPair keyPair = null;
		try {
			keyPair = KeyPairUtil.generateKeyPair(
				KEY_ALGORITHMS.get (
					getStringProperty (properties, KEY_ALGORITHM, KeyPairType.RSA.name ())
				), 
				getIntProperty (properties, KEY_SIZE, 512)
			);
		} catch (CryptoException e) {
			throw new CertificatesManagerException(e);
		}

		String commonName = getStringProperty(properties, COMMON_NAME,
				DUMMY_NAME);
		String organisationUnit = getStringProperty(properties,
				ORGANIZATION_UNIT, DUMMY_NAME);
		String organisationName = getStringProperty(properties,
				ORGANIZATION_NAME, DUMMY_NAME);
		String localityName = getStringProperty(properties, LOCALITY_NAME,
				DUMMY_NAME);
		String state = getStringProperty(properties, STATE, DUMMY_NAME);
		String country = getStringProperty(properties, COUNTRY, DUMMY_NAME);
		String email = getStringProperty(properties, DUMMY_EMAIL, DUMMY_NAME);
		int validity = getIntProperty(properties, VALIDITY, 365);
		SignatureType signatureType = SIGN_ALGORITHMS.get(getStringProperty(
				properties, SIGNATURE_ALGORITHM, SignatureType.MD5withRSA
						.name ()));

		String store = getStringProperty(properties, STORE, KeyStoreType.PKCS12
				.name ());
		String storePassword = getStringProperty(properties, KEY_PASSWORD,
				DUMMY_PASS);
		String alias = getStringProperty(properties, KEY_ALIAS,
				DEFAULT_ALIAS);

		X509Certificate cert;

		try {
			cert = X509CertUtil.generateCert(commonName, organisationUnit,
					organisationName, localityName, state, country, email,
					validity, keyPair.getPublic(), keyPair.getPrivate(),
					signatureType);
		} catch (CryptoException e) {
			throw new CertificatesManagerException(e);
		}

		KeyStore keyStore;
		try {
			
			keyStore = KeyStoreUtil.createKeyStore (STORE_TYPES.get(store));

			KeyStore.PrivateKeyEntry entry = 
				new KeyStore.PrivateKeyEntry (
					keyPair.getPrivate(), 
					new Certificate[] { cert }
			);

			KeyStore.PasswordProtection protection = 
				new KeyStore.PasswordProtection (storePassword.toCharArray());

			keyStore.setEntry (alias, entry, protection);

			keyStore.store (os, storePassword.toCharArray ());
			
		} catch (Throwable th) {
			throw new CertificatesManagerException (th);
		}

	}

	@Override
	public void export (KeyStore keyStore, Map<String, Object> properties, ExportFormat exportFormat, OutputStream os)
			throws CertificateExportException {
		
		boolean exportAll = getBooleanProperty (properties, EXPORT_ALL, false);
		
		String alias = getStringProperty (properties, KEY_ALIAS, DEFAULT_ALIAS);
		
		X509Certificate [] certs = null;
		
		try {
			if (exportAll) {
				certs = X509CertUtil.convertCertificates(keyStore.getCertificateChain (alias));
			} else {
				certs = new X509Certificate[] { 
					extractRootCertificate (keyStore, alias) 
				};
			}
			if (exportFormat.equals(ExportFormat.DER)) { // .cer
				os.write (certs [0].getEncoded ());
			} else if (exportFormat.equals(ExportFormat.PKCS7)) { // .p7b
				os.write (encode (certs, PKCS7_ENCODING));
			} else if (exportFormat.equals(ExportFormat.PKI_PATH)) { // .pkipath
				os.write (encode (certs, PKIPATH_ENCODING));
			} else if (exportFormat.equals(ExportFormat.PEM)) { // .pem
				JcaPEMWriter pw = null;
				try {
					pw = new JcaPEMWriter (new OutputStreamWriter(os));
					if (exportAll) {
						char [] password = getStringProperty (properties, KEY_PASSWORD, DUMMY_PASS).toCharArray ();
						pw.writeObject (keyStore.getKey (alias, password));
						for (Certificate cert : certs) {
							pw.writeObject (cert);
						}
					} else {
						pw.writeObject (certs [0]);
					}
					pw.flush();
				} finally {
					pw.close ();
				}
			} else if (exportFormat.equals (ExportFormat.PKCS10_CSR)) { // .csr
				JcaPEMWriter pw = null;
				try {
					char [] password = getStringProperty (properties, KEY_PASSWORD, DUMMY_PASS).toCharArray ();
					pw = new JcaPEMWriter (new OutputStreamWriter(os));
					pw.writeObject (X509CertUtil.generatePKCS10CSR (certs [0], (PrivateKey)keyStore.getKey (alias, password)));
					pw.flush();
				} finally {
					pw.close ();
				}
			}
		} catch (Throwable th) {
			throw new CertificateExportException (th);
		}
	}

	private static byte[] encode(X509Certificate[] certs, String encoding)
			throws CertificateException {
		return CertificateFactory.getInstance(X509).generateCertPath(
				Arrays.asList(certs)).getEncoded(encoding);
	}

	private X509Certificate extractRootCertificate (KeyStore keyStore, String alias) throws KeyStoreException, CryptoException {
		X509Certificate cert;
		if (keyStore.isKeyEntry (alias)) {
			cert = X509CertUtil.orderX509CertChain(X509CertUtil
					.convertCertificates(keyStore
							.getCertificateChain (alias)))[0];
		} else {
			cert = X509CertUtil.convertCertificate (keyStore.getCertificate (alias));
		}

		return cert;
	}

	@Override
	public KeyStore load (InputStream is, Map<String, Object> properties) throws StoreLoaderException {
		String store = getStringProperty (properties, STORE, KeyStoreType.PKCS12.name ());
		KeyStore keyStore = null;
		try {
			if (store == KeyStoreType.PKCS12.name ()) {
				// Prefer BC for PKCS #12 for now; the BC and SunJSSE 1.5+
				// implementations are incompatible in how they handle empty/missing passwords; BC works
				// consistently with char[0] on load and store (does not accept nulls), SunJSSE throws division by
				// zero with char[0] on load and store, works with null on load, does not work with null on store.
				// Checked with BC 1.{29,40}, SunJSSE 1.5.0_0{3,4,14}, 1.6.0 (OpenJDK)
				try {
					keyStore = KeyStore.getInstance (store, "BC");
				} catch (NoSuchProviderException ex) {
					// Fall through
				}
			}
			if (keyStore == null) {
				keyStore = KeyStore.getInstance (store);
			}
			String keyPassword = getStringProperty (properties, KEY_PASSWORD, DUMMY_PASS);
			keyStore.load (is, keyPassword.toCharArray ());
		} catch (Throwable th) {
			throw new StoreLoaderException (th);
		}
		return keyStore;
	}
}

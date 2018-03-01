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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

public class VerifyDocument {

	public static void main(String[] args) throws 	IOException, CertificateException, UnrecoverableKeyException, KeyStoreException,
													NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException, 
													CertStoreException, CMSException, OperatorCreationException {
		
		Security.addProvider (new org.bouncycastle.jce.provider.BouncyCastleProvider ());
		
		
		File f = new File ("Signed.pk7");
		byte[] buffer = new byte [(int)f.length ()];
		DataInputStream in = new DataInputStream (new FileInputStream (f));
		in.readFully (buffer);
		in.close ();

		CMSSignedData signature = new CMSSignedData (buffer);
		SignerInformation signer = (SignerInformation)signature.getSignerInfos ().getSigners ().iterator ().next ();
		
		// Added below
		Store<?> cs = signature.getCertificates ();
		Collection<?> matches = cs.getMatches (signer.getSID ());
		Iterator<?> iter = matches.iterator ();
		
		//CertStore cs = signature.getCertificatesAndCRLs ("Collection", "BC");
		//Iterator<? extends Certificate> iter = cs.getCertificates (signer.getSID ()).iterator ();
		
		JcaX509CertificateConverter converter = new JcaX509CertificateConverter ();
		converter.setProvider ("BC");
		X509Certificate certificate = converter.getCertificate ((X509CertificateHolder) iter.next ());
		
		CMSProcessable sc = signature.getSignedContent ();
		byte[] data = (byte[]) sc.getContent ();

		// Verify the signature
		//System.out.println (signer.verify (certificate, "BC"));
		System.out.println (new JcaSimpleSignerInfoVerifierBuilder ().setProvider ("BC").build (certificate));

		FileOutputStream envfos = new FileOutputStream ("Verified.txt");
		envfos.write (data);
		envfos.close ();
	}
}
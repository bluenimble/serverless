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
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;


public class SignDocument {
	
	static {
		Provider provider = Security.getProvider (BouncyCastleProvider.PROVIDER_NAME);
		if (provider == null) {
			Security.addProvider (new BouncyCastleProvider ());
		}
	}

	public static void main (String[] args) throws 	IOException, CertificateException, UnrecoverableKeyException, KeyStoreException,
			 										InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, 
			 										CertStoreException, CMSException, OperatorCreationException {
		
		File toBeSigned 	= new File ("ToBeSigned.txt");
		byte [] buffer 		= new byte [(int) toBeSigned.length ()];
		DataInputStream in 	= new DataInputStream (new FileInputStream (toBeSigned));
		in.readFully (buffer);
		in.close ();

		// Chargement des certificats qui seront stockes dans le fichier .p7
		// Ici, seulement le certificat personnal_nyal.cer sera associe.
		// Par contre, la chaîne des certificats non.
		X509Certificate cert = ReadX509.read (new FileInputStream ("msp.cer" /*"personnal_nyal.cer"*/));

		String password = "msp_pass"; 	// "2[$0wUOS";
		String alias 	= "msp"; 		// "thawte freemail member's thawte consulting (pty) ltd. id";

		KeyInformation keyInfo = ReadPKCS12.read (
			new FileInputStream ("msp.p12" /*"personnal_nyal.p12"*/), 
			password, 
			alias
		);

		// List<X509Certificate> certList = new ArrayList<X509Certificate> (); Wrong check below
		// certList.add (cert);
		List<X509CertificateHolder> certList = new ArrayList<X509CertificateHolder> ();
		certList.add (new X509CertificateHolder (cert.getEncoded ()));
		
		//CertStore certs = CertStore.getInstance ("Collection", new CollectionCertStoreParameters (certList), "BC"); Wrong check below
		JcaCertStore jcaCertStore = new JcaCertStore (certList);

		CMSSignedDataGenerator signGen = new CMSSignedDataGenerator ();
		
		ContentSigner contentSigner 					= new JcaContentSignerBuilder ("SHA1withRSA").setProvider ("BC").build (keyInfo.getPrivateKey ());
		DigestCalculatorProvider digestCalcProv 		= new JcaDigestCalculatorProviderBuilder ().setProvider ("BC").build ();
		SignerInfoGenerator signInfoGeneratorBuilder 	= new JcaSignerInfoGeneratorBuilder (digestCalcProv).build (contentSigner, cert);
		signGen.addSignerInfoGenerator (signInfoGeneratorBuilder);

		// privatekey correspond a notre cle privee recuperee du fichier PKCS#12
		// cert correspond au certificat publique personnal_nyal.cer
		// Le dernier argument est l'algorithme de hachage qui sera utilise
		//signGen.addSigner (keyInfo.getPrivateKey (), cert, CMSSignedDataGenerator.DIGEST_SHA1);
		
		signGen.addCertificates (jcaCertStore);
		// Wrong signGen.addCertificatesAndCRLs (certs);
		
		
		CMSProcessableByteArray content = new CMSProcessableByteArray (buffer);

		// Generation du fichier CMS/PKCS#7
		// L'argument deux permet de signifier si le document doit etre attache avec la signature
		// Valeur true: le fichier est attache (c'est le cas ici)
		// Valeur false: le fichier est detache
		//CMSSignedData signedData = signGen.generate (content, true, "BC");
		
		CMSSignedData signedData = signGen.generate (content, true);
		byte[] signeddata = signedData.getEncoded ();

		// Ecriture du buffer dans un fichier.
		FileOutputStream envfos = new FileOutputStream ("Signed.pk7");
		envfos.write (signeddata);
		envfos.close ();
	}
}
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.util.Collection;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.bluenimble.platform.crypto.tests.KeyInformation;
import com.bluenimble.platform.crypto.tests.ReadPKCS12;

public class DecryptDocument {
	
	static {
		Provider provider = Security.getProvider (BouncyCastleProvider.PROVIDER_NAME);
		if (provider == null) {
			Security.addProvider (new BouncyCastleProvider ());
		}
	}

	public static void main (String[] args) throws 	IOException, CertificateException, UnrecoverableKeyException, KeyStoreException,
													NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException, 
													CertStoreException, CMSException, NoSuchPaddingException, InvalidKeyException, 
													ShortBufferException, IllegalBlockSizeException, BadPaddingException {

		CMSEnvelopedData ced 	= new CMSEnvelopedData (new FileInputStream ("ToBeDecrypted.pk7"));
		Collection<?> recip 	= ced.getRecipientInfos ().getRecipients ();

		KeyTransRecipientInformation rinfo = (KeyTransRecipientInformation) recip.iterator ().next ();
		
		// privatekey est la clé privée permettant de déchiffrer la clé secrète (symétrique)
		String password = "bspass"; 	// "2[$0wUOS";
		String alias 	= "bscert"; 	// "thawte freemail member's thawte consulting (pty) ltd. id";
		
		KeyInformation keyInfo = ReadPKCS12.read (
			new FileInputStream ("files/test.p12"), 
			password, 
			alias
		);
		
		byte [] contents = rinfo.getContent (new JceKeyTransEnvelopedRecipient (keyInfo.getPrivateKey ()).setProvider("BC"));

		FileOutputStream envfos = new FileOutputStream ("Decrypted.txt");
		envfos.write (contents);
		envfos.close ();
	}
}
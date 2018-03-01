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
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.RecipientInfoGenerator;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;

public class EncryptDocument {

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
		
		File f 				= new File ("ToBeEncrypted.txt");
        byte[] buffer 		= new byte [(int)f.length ()];
        DataInputStream in 	= new DataInputStream (new FileInputStream (f));
        in.readFully (buffer);
        in.close ();

        // Chiffrement du document

        // La variable cert correspond au certificat du destinataire
        // La clé publique de ce certificat servira à chiffrer la clé symétrique
        X509Certificate cert = ReadX509.read (new FileInputStream ("files/test.cer"));
        
        CMSEnvelopedDataGenerator gen = new CMSEnvelopedDataGenerator ();
        RecipientInfoGenerator recipientGenerator = new JceKeyTransRecipientInfoGenerator (cert).setProvider ("BC");
        gen.addRecipientInfoGenerator (recipientGenerator);

        // Choix de l'algorithme à clé symétrique pour chiffrer le document.
        // AES est un standard. Vous pouvez donc l'utiliser sans crainte.
        // Il faut savoir qu'en france la taille maximum autorisée est de 128 bits pour les clés symétriques (ou clés secrètes)
        OutputEncryptor outputEncryptor = new JceCMSContentEncryptorBuilder (CMSAlgorithm.AES128_CBC).build (); 
        CMSEnvelopedData envData 		= gen.generate (new CMSProcessableByteArray (buffer), outputEncryptor);

        byte[] pkcs7envelopedData 		= envData.getEncoded ();

        // Ecriture du document chiffré
        FileOutputStream envfos = new FileOutputStream ("ToBeDecrypted.pk7");
        envfos.write (pkcs7envelopedData);
        envfos.close ();
	}
}
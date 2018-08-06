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
package com.bluenimble.platform;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.bluenimble.platform.encoding.Base64;

public class Crypto {
	
	private static final String Provider = "SunJCE";
	
	public enum Algorithm {
		AES,
		DES
	}
	
	public enum Hmac {
		SHA1,
		SHA256
	}
	
	public enum Hashing {
		HEXA,
		BASE64
	}
	
	public static String md5 (String data, String charset) throws UnsupportedEncodingException {
		if (Lang.isNullOrEmpty (charset)) {
			charset = Encodings.UTF8;
		}
		return md5 (data.getBytes (charset));
	}
	private static String md5 (byte [] data) {
        MessageDigest m;
		try {
			m = MessageDigest.getInstance ("MD5");
		} catch (NoSuchAlgorithmException e) {
			return new String (data);
		}
        m.update (data, 0, data.length);
        BigInteger i = new BigInteger (1, m.digest ());
        return String.format ("%1$032X", i);
	}
	
	public static String hmac (byte [] data, String key, Hmac hmac, Hashing hashing) throws Exception {
		byte[] secretKeyBytes = key.getBytes (Encodings.UTF8);
		
		SecretKeySpec secretKeySpec =
	      new SecretKeySpec (secretKeyBytes, Hmac.class.getSimpleName () + hmac.name ());
		
		Mac mac = Mac.getInstance (Hmac.class.getSimpleName () + hmac.name ());
	    mac.init (secretKeySpec);
	    
		byte [] rawHmac = mac.doFinal (data);
		
		if (Hashing.BASE64.equals (hashing)) {
			return new String (Base64.encodeBase64 (rawHmac), Encodings.UTF8).trim ();
		} 
		
		return toHexa (rawHmac);
	}
	
	private static String toHexa (byte [] bytes) {
        StringBuilder sb = new StringBuilder ();
        for (int i = 0; i < bytes.length; i++) {
            int num = (bytes[i] & 0xff);
            String hex = Integer.toHexString (num);
            if (hex.length () == 1) {
                hex = "0" + hex;
            }
            sb.append (hex);
        }
        String s = sb.toString ();
        sb.setLength (0);
        sb = null;
        return s;
    }
	
	private static byte [] crypt (byte [] data, String key, Algorithm algorithm, int mode) throws Exception {
		Key secretKey = getSecretKey (key, algorithm);
		Cipher cipher = Cipher.getInstance (algorithm.name (), Provider);
		cipher.init (mode, secretKey);
		return cipher.doFinal (data);
	}
	
	public static byte [] decrypt (byte [] data, String key, Algorithm algorthm) throws Exception {
		return crypt (data, key, algorthm, Cipher.DECRYPT_MODE);
	}
	
	public static byte [] encrypt (byte [] data, String key, Algorithm algorthm) throws Exception {
		return crypt (data, key, algorthm, Cipher.ENCRYPT_MODE);
	}
	
	private static SecretKey getSecretKey (String key, Algorithm algorithm) throws Exception {
		switch (algorithm) {
			case AES:
				int length = key.length ();  
		        if (length > 16 && length != 16){  
		        	key = key.substring (0, 15);  
		        }  
		        if (length < 16 && length != 16){  
		             for(int i = 0; i < 16 - length; i++){  
		            	 key = key + "0";  
		             }  
		        }
		        return new SecretKeySpec (key.getBytes (), algorithm.name ());
	
			case DES:
				return SecretKeyFactory.getInstance (algorithm.name ()).generateSecret (new DESKeySpec (key.getBytes ()));
	
			default:
				return null;
		}
	}
	
	public static void main (String [] args) throws Exception {
		/*
		byte [] encrypted = Crypto.encrypt ("Im@ne1977".getBytes (), "a35T@,#;_", Algorithm.AES);
		
		String encryptedEncoded = Base64.encodeBase64String (encrypted);
		
		
		System.out.print ("Encrypted-Encoded " + encryptedEncoded);
		
		byte [] encryptedDecoded = Base64.decodeBase64 (encryptedEncoded.getBytes ());
		
		byte [] decrypted = Crypto.decrypt (encryptedDecoded, "a35T@,#;_", Algorithm.AES);
		
		System.out.print ("Decrypted-Decoded " + new String (decrypted));
		*/
		
		System.out.println (Crypto.md5 ("Im@ne1977", Encodings.UTF8));
		
	}
	
}

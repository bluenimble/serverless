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
package com.bluenimble.platform.http.utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.bluenimble.platform.encoding.Base64;

public class DataSigner {
	
	public static final 	String ISO  = "iso-8859-1";
	public static final 	String UTF8 = "UTF-8";

    public static final 	String HEXA_ALGORITHM         	= "hexa";
    public static final 	String BASE64_ALGORITHM       	= "base64";
    
    public static final 	String DEFAULT_SIGN_ALGORITHM 	= "HmacSHA256";
	public static final 	String DEFAULT_HASH_ALGORITHM 	= BASE64_ALGORITHM;
    
    private static final 	String HMAC 					= "HMAC";


    public static String sign (byte [] content, byte [] key)  
           throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        return sign (content, key, null);
    }
    
    public static String sign (byte [] content, byte [] key, String signAlgorithm)  
           throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        return sign (content, key, signAlgorithm, null);
    }
    
    public static String sign (byte [] content, byte [] key, String signAlgorithm, String hashAlgorithm)  
           throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        return sign (content, key, signAlgorithm, hashAlgorithm, null);
    }
    
    public static String sign (byte [] content, byte [] key, String signAlgorithm, String hashAlgorithm, String encoding) 
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
         if (signAlgorithm == null) {
             signAlgorithm = DEFAULT_SIGN_ALGORITHM;
         }
         if (hashAlgorithm == null) {
             hashAlgorithm = DEFAULT_HASH_ALGORITHM;
         }
         if (encoding == null) {
             encoding = UTF8;
         }
         byte [] data = null;
     	
         if (signAlgorithm.toUpperCase ().startsWith (HMAC)) {
             data = signHMac (content, signAlgorithm, key, encoding);
         } else {
             MessageDigest md = MessageDigest.getInstance (signAlgorithm);
             data = md.digest (content);
         }
         return encode (data, hashAlgorithm, encoding);
     }
     
     private static byte [] signHMac (byte [] content, String signAlgorithm, byte [] key, String encoding) throws NoSuchAlgorithmException, InvalidKeyException {
     	SecretKey skey = new SecretKeySpec (key, signAlgorithm);
         Mac mac = Mac.getInstance (signAlgorithm);
         mac.init (skey);
         return mac.doFinal (content);
     }
     
     public static String encode (byte [] bytes, String hashAlgorithm, String encoding)
             throws UnsupportedEncodingException {
     	
         if (BASE64_ALGORITHM.equals (hashAlgorithm)) {
         	Base64 encoder = new Base64 ();
 			return new String (encoder.encode (bytes), encoding).trim ();
         } else if (HEXA_ALGORITHM.equals (hashAlgorithm)) {
             return toHexa (bytes);
         } 
         return new String (bytes, encoding);
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
    
}
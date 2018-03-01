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
package com.bluenimble.platform.crypto.signer.impl;


import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.bluenimble.platform.crypto.Algorithm;
import com.bluenimble.platform.crypto.Base64;
import com.bluenimble.platform.crypto.SecureDocument;
import com.bluenimble.platform.crypto.SignatureAware;
import com.bluenimble.platform.crypto.signer.CertificateAcceptor;
import com.bluenimble.platform.crypto.signer.Signer;
import com.bluenimble.platform.crypto.signer.SignerException;
import com.bluenimble.platform.crypto.signer.StringKey;

public class SimpleSigner implements Signer {

	private static final long serialVersionUID = 1518831772847103466L;
	
	private static final char [] digits = {'0', '1', '2', '3', '4','5','6','7','8','9','a','b','c','d','e','f'};
	public static final String BASE64_ENCODING = "ISO-8859-1";

	@Override
	public void sign (SecureDocument doc, Key key, X509Certificate [] certs) throws SignerException {
		if (key == null) {
			throw new SignerException ("Null Private Key not allowed");
		}
		if (SecretKey.class.isAssignableFrom (key.getClass ())) {
			signWithKey (doc, (SecretKey)key);
		} else if (StringKey.class.isAssignableFrom (key.getClass ())) {
			signWithKey (doc, new SecretKeySpec (key.getEncoded (), key.getAlgorithm ()));
		}
	}

	private void signWithKey (SecureDocument doc, SecretKey key) throws SignerException {
		try {
			byte [] signature = null;
			if (key.getAlgorithm ().equals (Algorithm.HmacSHA1.getId ())) {
		    	Mac mac = Mac.getInstance (key.getAlgorithm ());
				mac.init (key);
			    signature = doc.getBytes ();
			    if (SignatureAware.class.isAssignableFrom (doc.getClass ())) {
		        	((SignatureAware)doc).setSignature (new Base64().encode (mac.doFinal (signature)));
		        } else {
		        	doc.setBytes (mac.doFinal (signature));
		        }
	        } else {
				MessageDigest md = MessageDigest.getInstance (key.getAlgorithm ());
				signature = md.digest (doc.getBytes ());
				if (SignatureAware.class.isAssignableFrom (doc.getClass ())) {
		        	((SignatureAware)doc).setSignature (hexEncode (signature));
		        } else {
		        	doc.setBytes (hexEncode (signature));
		        }
		    }
		} catch (Throwable th) {
			throw new SignerException (th, th.getMessage ());
		}
	}
	
	@Override
	public void verify (SecureDocument doc, CertificateAcceptor acceptor) throws SignerException {
		try {
			if (SignatureAware.class.isAssignableFrom (doc.getClass ())) {
				SignatureAware signed = (SignatureAware)doc;
	        	byte [] signature = signed.getSignature ();
	        	if (signature == null) {
	        		throw new SignerException ("Signature not found in document");
	        	}
	        	Key key = signed.getKey ();   
	        	if (key == null) {
	        		throw new SignerException ("Secret key not found in document");
	        	}
	        	sign (doc, key, null);
	        	byte [] expected = ((SignatureAware)doc).getSignature ();
	        	if (!equals (signature, expected)) {
	        		throw new SignerException ("Invalid signature");
	        	}
	        }
		} catch (Throwable th) {
			throw new SignerException (th, th.getMessage ());
		}
	}
	
	protected byte [] hexEncode (byte[] aInput) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		for ( int idx = 0; idx < aInput.length; ++idx) {
			byte b = aInput[idx];
			result.append( digits[ (b&0xf0) >> 4 ] );
			result.append( digits[ b&0x0f] );
		}
		String sig = result.toString ();
		return sig.getBytes (BASE64_ENCODING);
	}
	
	protected boolean equals (String x, String y) {
        if (x == null)
            return y == null;
        else if (y == null)
            return false;
        else if (y.length() <= 0)
            return x.length() <= 0;
        char[] a = x.toCharArray();
        char[] b = y.toCharArray();
        char diff = (char) ((a.length == b.length) ? 0 : 1);
        int j = 0;
        for (int i = 0; i < a.length; ++i) {
            diff |= a[i] ^ b[j];
            j = (j + 1) % b.length;
        }
        return diff == 0;
    }

    protected boolean equals (byte[] a, byte[] b) {
        if (a == null)
            return b == null;
        else if (b == null)
            return false;
        else if (b.length <= 0)
            return a.length <= 0;
        byte diff = (byte) ((a.length == b.length) ? 0 : 1);
        int j = 0;
        for (int i = 0; i < a.length; ++i) {
            diff |= a[i] ^ b[j];
            j = (j + 1) % b.length;
        }
        return diff == 0;
    }

}

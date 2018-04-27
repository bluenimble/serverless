package com.bluenimble.platform.security;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.bluenimble.platform.Lang;

public class SslUtils {
	
	private static final String TLS = "TLS";

	public interface StoreSource {
		String		type 		();
		String		algorithm 	();
		InputStream stream 		();
		char [] 	password 	();
		void 		close 		();
	}

	public static SSLContext sslContext (StoreSource keystore, StoreSource truststore) throws Exception {
		try {
			
			KeyManager [] kms = null;
			TrustManager [] tms = null;
			
			// load keystore
			if (keystore != null) {
				KeyStore ks = load (keystore);

				String kalg = keystore.algorithm ();
				if (Lang.isNullOrEmpty (kalg)) {
					kalg = KeyManagerFactory.getDefaultAlgorithm ();
				}
				KeyManagerFactory kmf = KeyManagerFactory.getInstance (kalg);
				kmf.init (ks, keystore.password ());
				
				kms = kmf.getKeyManagers ();
			}
			
			// load truststore
			if (truststore != null) {
				KeyStore ts = load (truststore);;
	
				String talg = truststore.algorithm ();
				if (Lang.isNullOrEmpty (talg)) {
					talg = TrustManagerFactory.getDefaultAlgorithm ();
				}
				TrustManagerFactory tmf = TrustManagerFactory.getInstance (talg);
				tmf.init (ts);
				
				tms = tmf.getTrustManagers ();
			}

			SSLContext sslContext = SSLContext.getInstance (TLS);
			sslContext.init (kms, tms, new SecureRandom ());
			
			return sslContext;
			
		} finally {
			keystore.close ();
			truststore.close ();
		}
		
	}
	
	public static KeyStore load (StoreSource store) throws Exception {
		KeyStore s = KeyStore.getInstance (store.type ());
		s.load (store.stream (), store.password ());
		return s;
	}
	
}

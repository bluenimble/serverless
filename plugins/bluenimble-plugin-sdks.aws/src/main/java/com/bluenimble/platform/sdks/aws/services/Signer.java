package com.bluenimble.platform.sdks.aws.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.PEM;
import com.amazonaws.auth.RSA;
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;

public class Signer {
	
	interface Spec {
		String Protocol 	= "protocol";
		String Domain		= "domain";
		String PrivateKey	= "privateKey";
		String KeyPairId	= "keyPairId";
		String Age			= "age";
	}
	
	public Signer () {
	}
	
	public PrivateKey loadPrivateKey(File privateKeyFile) throws InvalidKeySpecException, IOException {
        if ( StringUtils.lowerCase(privateKeyFile.getAbsolutePath()).endsWith(".pem") ) {
            InputStream is = new FileInputStream(privateKeyFile);
            try {
                return PEM.readPrivateKey(is);
            } finally {
                try {is.close();} catch(IOException ignore) {}
            }
        } else if ( StringUtils.lowerCase(privateKeyFile.getAbsolutePath()).endsWith(".der") ) {
            InputStream is = new FileInputStream(privateKeyFile);
            try {
                return RSA.privateKeyFromPKCS8(IOUtils.toByteArray(is));
            } finally {
                try {is.close();} catch(IOException ignore) {}
            }
        } else {
            throw new AmazonClientException("Unsupported file type for private key");
        }
    }
	
	public String sign (String resource, long age) throws Exception {
		
		PrivateKey privateKey = this.loadPrivateKey (new File ("/Users/lilya/Downloads/pk-APKAJFA5LSCSI6NNEHMQ.pem"));
		
		return CloudFrontUrlSigner.getSignedURLWithCannedPolicy ("https://alpha.com",
				"APKAJFA5LSCSI6NNEHMQ", privateKey, new Date ());
		
		/*
		return CloudFrontUrlSigner.getSignedURLWithCannedPolicy (
			Protocol.valueOf (Json.getString (spec, Spec.Protocol, Protocol.https.name ())), 
			Json.getString (spec, Spec.Domain), 
			new File (Json.getString (spec, Spec.PrivateKey)),
			resource,
			Json.getString (spec, Spec.KeyPairId), 
			new Date (System.currentTimeMillis () + age)
		);
		*/
		
	}
	
	public static void main (String [] args) throws Exception {
		// System.out.println (new Signer ().sign ("alpha", 0));
		
		Calendar date = Calendar.getInstance();
		System.out.println("Current Date and TIme : " + date.getTime());
		long timeInSecs = date.getTimeInMillis();
		Date afterAdding10Mins = new Date(timeInSecs + (10 * 60 * 1000));
		System.out.println("After adding 10 mins : " + afterAdding10Mins);
	}
	
	
	
}

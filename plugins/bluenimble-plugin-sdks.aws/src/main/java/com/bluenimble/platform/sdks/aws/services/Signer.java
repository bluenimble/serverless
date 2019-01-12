package com.bluenimble.platform.sdks.aws.services;

import java.io.File;
import java.util.Date;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils.Protocol;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonObject;

public class Signer {
	
	interface Spec {
		String Protocol 	= "protocol";
		String Domain		= "domain";
		String PrivateKey	= "privateKey";
		String KeyPairId	= "keyPairId";
		String Age			= "age";
	}
	
	private JsonObject spec;

	public Signer (JsonObject spec) {
		this.spec = spec;
	}
	
	public String sign (String resource, long age) throws Exception {
		return CloudFrontUrlSigner.getSignedURLWithCannedPolicy (
			Protocol.valueOf (Json.getString (spec, Spec.Protocol, Protocol.https.name ())), 
			Json.getString (spec, Spec.Domain), 
			new File (Json.getString (spec, Spec.PrivateKey)),
			resource,
			Json.getString (spec, Spec.KeyPairId), 
			new Date (System.currentTimeMillis () + age)
		);
	}
	
}

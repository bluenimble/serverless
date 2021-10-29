package com.bluenimble.platform.sdks.aws.services.cf;

import java.io.InputStream;
import java.security.PrivateKey;
import java.util.Calendar;
import java.util.Date;

import com.amazonaws.auth.PEM;
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;

public class CloudFrontUriSignerServiceSpi extends AbstractApiServiceSpi {
	
	private static final long serialVersionUID = -4984270801557542635L;

	private String Key = "key";
	
	interface Spec {
		String Resource = "resource";
		String LifeTime	= "lifeTime";
	}
	interface Output {
		String Signed 	= "signed";
	}
	
	private PrivateKey privateKey;

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		return new JsonApiOutput (
			(JsonObject)new JsonObject ().set (
				Output.Signed, 
				CloudFrontUrlSigner.getSignedURLWithCannedPolicy (
					(String)request.get (Spec.Resource, ApiRequest.Scope.Parameter),
					Json.getString (request.getService ().getSpiDef (), Key),
					privateKey,
					new Date (
						Calendar.getInstance ().getTimeInMillis () + 
						((long)request.get (Spec.LifeTime, ApiRequest.Scope.Parameter) * 1000)
					)
				)
			)
		);
	}
	
	@Override
	public void onStart (Api api, ApiService service, ApiContext context) throws ApiManagementException {
		String key = Json.getString (service.getSpiDef (), Key);
		
		ApiResource privateKeyResource;
		try {
			privateKeyResource = api.getResourcesManager ().get (new String [] {"keys", key + ".pem"});
		} catch (ApiResourcesManagerException ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
		if (privateKeyResource == null) {
			return;
		}
		InputStream stream = null;
		try {
			privateKey = PEM.readPrivateKey (privateKeyResource.toInput ());
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (stream);
		}
	}

}

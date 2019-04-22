package com.bluenimble.platform.api.impls.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.ValueHolder;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiServiceSpi;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.Remote;

public class RemoteApiServiceSpi implements ApiServiceSpi {

	private static final long serialVersionUID = -7448782202115381461L;
	
	private static final String SpiKey 	= Api.Spec.Spi.class.getSimpleName ().toLowerCase ();

	interface Spec {
		String Remote = "remote";
	}
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {

		String remoteId = (String)Json.find (request.getService ().toJson (), SpiKey, Spec.Remote);
		if (Lang.isNullOrEmpty (remoteId)) {
			throw new ApiServiceExecutionException (Spec.Remote + " not defined in service " + SpiKey);
		}
		
		Remote remote = api.space ().feature (Remote.class, remoteId, request);
		
		JsonObject spec = new JsonObject ();
		
		// set path
		spec.set (Remote.Spec.Path, request.getPath ());

		// add headers
		Iterator<String> hkeys = request.keys (Scope.Header);
		if (hkeys != null) {
			JsonObject headers = new JsonObject ();
			spec.set (Remote.Spec.Headers, headers);
			while (hkeys.hasNext ()) {
				String hkey = hkeys.next ();
				headers.set (hkey, request.get (hkey, Scope.Header));
			}
		}
		
		// add data
		// add headers
		Iterator<String> pkeys = request.keys (Scope.Parameter);
		if (pkeys != null) {
			JsonObject data = new JsonObject ();
			spec.set (Remote.Spec.Data, data);
			while (pkeys.hasNext ()) {
				String pkey = pkeys.next ();
				data.set (pkey, request.get (pkey, Scope.Parameter));
			}
		}
		
		OutputStream out = null;
		try {
			out = response.toOutput ();
		} catch (IOException e) {
			throw new ApiServiceExecutionException (e.getMessage (), e);
		}
		
		final ValueHolder<Boolean> vhChunked = new ValueHolder<Boolean> ();
		
		final OutputStream fOut = out;
		
		Remote.Callback callback = new Remote.Callback () {
			@Override
			public void onStatus (int status, boolean chunked, Map<String, Object> headers) {
				vhChunked.set (chunked);
				if (headers == null || headers.isEmpty ()) {
					return;
				}
				for (String h : headers.keySet ()) {
					response.set (h, headers.get (h));
				}
				response.flushHeaders ();
			}
			@Override
			public void onError (int status, Object message) throws IOException {
				response.write (message);
			}
			@Override
			public void onData (int status, byte [] chunk) throws IOException {
				if (!vhChunked.get () || chunk == null || chunk.length == 0) {
					return;
				}
				fOut.write (chunk);
			}
			@Override
			public void onDone (int code, Object data) throws IOException {
				if (!vhChunked.get () && data != null) {
					if (data instanceof InputStream) {
						IOUtils.copy ((InputStream)data, fOut);
					} else {
						response.write (data);
					}
				}
				response.close ();
			}
		};
		
		ApiVerb verb = request.getVerb ();
		switch (verb) {
			case GET:
				remote.get (spec, callback);
				break;
	
			case POST:
				remote.post (spec, callback);
				break;
	
			case PUT:
				remote.put (spec, callback);
				break;
	
			case DELETE:
				remote.delete (spec, callback);
				break;
	
			case PATCH:
				remote.patch (spec, callback);
				break;
	
			default:
				break;
		}
		
		return null;
	}

	@Override
	public void onResolve (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response) 
		throws ApiServiceExecutionException {
		
	}

	@Override
	public void onStart (Api api, ApiService service, ApiContext context) throws ApiManagementException {
		
	}

	@Override
	public void onStop (Api api, ApiService service, ApiContext context) throws ApiManagementException {
		
	}

}

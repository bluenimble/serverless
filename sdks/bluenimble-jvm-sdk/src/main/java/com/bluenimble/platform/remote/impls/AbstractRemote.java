package com.bluenimble.platform.remote.impls;

import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.Remote;

public abstract class AbstractRemote implements Remote {
	
	private static final long serialVersionUID = -986635551128538160L;

	protected interface Signers {
		String Bnb 		= "bnb";
		String OAuth 	= "oauth";
		String Basic	= "basic";
	}
	
	protected JsonObject featureSpec;
	
	@Override
	public void post (JsonObject spec, Callback callback, ApiStreamSource... attachments) {
		request (ApiVerb.POST, spec, callback);
	}

	@Override
	public void put (JsonObject spec, Callback callback, ApiStreamSource... attachments) {
		request (ApiVerb.PUT, spec, callback);
	}

	@Override
	public void get (JsonObject spec, Callback callback) {
		request (ApiVerb.GET, spec, callback);
	}

	@Override
	public void delete (JsonObject spec, Callback callback) {
		request (ApiVerb.DELETE, spec, callback);
	}

	@Override
	public void head (JsonObject spec, Callback callback) {
		request (ApiVerb.HEAD, spec, callback);
	}

	@Override
	public void patch (JsonObject spec, Callback callback) {
		request (ApiVerb.PATCH, spec, callback);
	}
	
	@Override
	public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
		
	}

	@Override
	public Object get () {
		return null;
	}
	
	public abstract void request (ApiVerb verb, JsonObject spec, Callback callback, ApiStreamSource... attachments);

}

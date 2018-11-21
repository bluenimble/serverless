package com.bluenimble.platform.api.media.impls;

import java.io.IOException;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.media.DataWriter;
import com.bluenimble.platform.json.AbstractEmitter;
import com.bluenimble.platform.json.JsonEmitter;
import com.bluenimble.platform.json.JsonObject;

public class JsonWriter implements DataWriter {

	private static final long serialVersionUID = -8591385465996026292L;
	
	private String defaultResponse = Lang.EMTPY_OBJECT;
	
	public JsonWriter () {
	}
	public JsonWriter (String defaultResponse) {
		this.defaultResponse = defaultResponse;
	}

	@Override
	public void write (Api api, ApiService service, ApiOutput output, ApiResponse response) throws IOException {
		
		if (output == null) {
			if (defaultResponse != null) {
				response.write (defaultResponse);
			}
			return;
		} 
		
		JsonObject json = output.data ();
		if (json == null) {
			if (defaultResponse != null) {
				response.write (defaultResponse);
			}
			return;
		} 
		
		boolean cast = false;

		Object oCast = output.get (ApiOutput.Defaults.Cast);
		if (oCast == null) {
			cast = Json.getBoolean (
				service.getMedia (), 
				ApiService.Spec.Media.Cast, 
				Json.getBoolean (api.getMedia (), Api.Spec.Media.Cast, true)
			);
		} else {
			cast = oCast instanceof Boolean && ((Boolean)oCast);
		}
		
		json.write (new AbstractEmitter () {
			@Override
			public JsonEmitter write (String chunk) {
				try {
					response.write (chunk);
				} catch (IOException e) {
					throw new RuntimeException (e.getMessage (), e);
				}
				return this;
			}
		}.cast (cast));
		
	}

}

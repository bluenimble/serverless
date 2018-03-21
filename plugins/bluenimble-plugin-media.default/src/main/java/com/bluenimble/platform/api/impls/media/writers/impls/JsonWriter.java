package com.bluenimble.platform.api.impls.media.writers.impls;

import java.io.IOException;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.media.DataWriter;
import com.bluenimble.platform.json.AbstractEmitter;
import com.bluenimble.platform.json.JsonEmitter;
import com.bluenimble.platform.json.JsonObject;

public class JsonWriter implements DataWriter {

	private static final long serialVersionUID = -8591385465996026292L;

	@Override
	public void write (ApiOutput output, ApiResponse response) throws IOException {
		
		if (output == null) {
			response.write (Lang.EMTPY_OBJECT);
			return;
		} 
		
		JsonObject json = output.data ();
		if (json == null) {
			response.write (Lang.EMTPY_OBJECT);
			return;
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
		});
		
	}

}

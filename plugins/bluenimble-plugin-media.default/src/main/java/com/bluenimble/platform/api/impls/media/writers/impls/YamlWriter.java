package com.bluenimble.platform.api.impls.media.writers.impls;

import java.io.IOException;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.media.DataWriter;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.json.printers.YamlOutputStreamPrinter;

public class YamlWriter implements DataWriter {

	private static final long serialVersionUID = -8591385465996026292L;

	@Override
	public void write (Api api, ApiService service, ApiOutput output, ApiResponse response) throws IOException {
		
		response.flushHeaders ();
		
		if (output == null) {
			response.write (Lang.BLANK);
			return;
		} 
		
		JsonObject json = output.data ();
		if (json == null) {
			response.write (Lang.BLANK);
			return;
		} 
		
		new YamlOutputStreamPrinter (response.toOutput ()).print (json);

	}

}

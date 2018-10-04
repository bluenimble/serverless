package com.bluenimble.platform.api.media.impls;

import java.io.IOException;
import java.io.OutputStream;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.media.DataWriter;

public class TextWriter implements DataWriter {

	private static final long serialVersionUID = -8591385465996026292L;

	@Override
	public void write (ApiOutput output, ApiResponse response) throws IOException {
		
		if (output == null) {
			response.write (Lang.BLANK);
			response.close ();
			return;
		} 
		
		OutputStream ros = response.toOutput ();
		
		response.flushHeaders ();
		
		output.pipe (ros, 0, -1);
		
	}

}

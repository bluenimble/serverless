package com.bluenimble.platform.api.media;

import java.io.IOException;
import java.io.Serializable;

import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiResponse;

public interface DataWriter extends Serializable {
	
	void write (ApiOutput output, ApiResponse response) throws IOException;
	
}

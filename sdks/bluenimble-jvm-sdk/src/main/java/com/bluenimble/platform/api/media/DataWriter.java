package com.bluenimble.platform.api.media;

import java.io.IOException;
import java.io.Serializable;

import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;

public interface DataWriter extends Serializable {
	
	void write (Api api, ApiService service, ApiOutput output, ApiResponse response) throws IOException;
	
}

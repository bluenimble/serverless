package com.bluenimble.platform.tools.binary;

import java.util.Map;

import com.bluenimble.platform.api.ApiResponse;

public interface Callback {
	
	void onStatus 	(ApiResponse.Status status);
	void onHeaders 	(Map<String, Object> headers);
	void onChunk 	(byte [] chunk);
	void onFinish 	();
	
}
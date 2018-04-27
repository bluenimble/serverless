package com.bluenimble.platform.icli.mgm.remote.binary;

import java.util.Map;

import com.bluenimble.platform.api.ApiResponse;

public interface ResponseCallback {
	
	void onStatus 	(ApiResponse.Status status);
	void onHeaders 	(Map<String, Object> headers);
	void onChunk 	(byte [] chunk);
	void onFinish 	();
	
}
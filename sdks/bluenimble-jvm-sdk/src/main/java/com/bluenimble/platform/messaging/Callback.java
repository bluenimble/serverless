package com.bluenimble.platform.messaging;

import java.io.Serializable;

import com.bluenimble.platform.json.JsonObject;

public interface Callback extends Serializable {

	void process (JsonObject feedback);
	
}

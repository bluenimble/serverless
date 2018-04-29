package com.bluenimble.platform.tools.binary;

import java.io.Serializable;

public interface BinaryClientFactory extends Serializable {

	BinaryClient create (Callback callback);
	
}

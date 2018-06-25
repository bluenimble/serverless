package com.bluenimble.platform.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public interface ApiRequestBodyReader extends Serializable {
	
	Object 		read 		(InputStream proxy) throws IOException;
	String []	mediaTypes 	();
	
}

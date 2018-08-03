package com.bluenimble.platform.api.impls.readers;

import java.io.IOException;
import java.io.InputStream;

import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequestBodyReader;
import com.bluenimble.platform.api.impls.DefaultApiStreamSource;

public class StreamApiRequestBodyReader implements ApiRequestBodyReader {

	private static final long serialVersionUID = -9161966870378744014L;

	@Override
	public Object read (InputStream payload, String contentType) throws IOException {
		return new DefaultApiStreamSource (ApiRequest.Payload, ApiRequest.Payload, contentType, payload).setClosable (true);
	}

	@Override
	public String [] mediaTypes () {
		return new String [] { ApiContentTypes.Stream };
	}

}

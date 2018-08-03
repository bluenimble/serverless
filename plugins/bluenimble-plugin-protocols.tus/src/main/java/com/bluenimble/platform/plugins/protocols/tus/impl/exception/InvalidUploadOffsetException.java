package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

public class InvalidUploadOffsetException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public InvalidUploadOffsetException(String message) {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }
}

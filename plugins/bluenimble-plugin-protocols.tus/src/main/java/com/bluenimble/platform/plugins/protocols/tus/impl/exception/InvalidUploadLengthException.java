package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception thrown when no valid Upload-Length or Upload-Defer-Length header is found
 */
public class InvalidUploadLengthException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public InvalidUploadLengthException(String message) {
        super(HttpServletResponse.SC_BAD_REQUEST, message);
    }
}

package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception thrown when the Upload-Concat header contains an ID which is not valid
 */
public class InvalidPartialUploadIdException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public InvalidPartialUploadIdException(String message) {
        super(HttpServletResponse.SC_PRECONDITION_FAILED, message);
    }
}

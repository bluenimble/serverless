package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception thrown when the given upload length exceeds or internally defined maximum
 */
public class MaxUploadLengthExceededException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public MaxUploadLengthExceededException(String message) {
        super(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, message);
    }
}

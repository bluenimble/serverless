package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception thrown when the Client includes the Upload-Length header in the upload creation.
 */
public class UploadLengthNotAllowedOnConcatenationException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public UploadLengthNotAllowedOnConcatenationException(String message) {
        super(HttpServletResponse.SC_BAD_REQUEST, message);
    }
}

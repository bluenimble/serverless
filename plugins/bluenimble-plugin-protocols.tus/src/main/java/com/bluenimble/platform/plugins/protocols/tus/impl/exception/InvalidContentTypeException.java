package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception thrown when the request has an invalid content type.
 */
public class InvalidContentTypeException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public InvalidContentTypeException(String message) {
        super(HttpServletResponse.SC_NOT_ACCEPTABLE, message);
    }
}

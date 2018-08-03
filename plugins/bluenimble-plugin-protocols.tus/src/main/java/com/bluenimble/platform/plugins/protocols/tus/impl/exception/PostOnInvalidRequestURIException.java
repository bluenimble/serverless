package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception thrown when a POST request was received on an invalid URI
 */
public class PostOnInvalidRequestURIException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public PostOnInvalidRequestURIException(String message) {
        super(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
    }
}

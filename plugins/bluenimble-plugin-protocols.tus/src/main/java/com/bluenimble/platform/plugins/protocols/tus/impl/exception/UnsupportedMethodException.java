package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception thrown when we receive a HTTP request with a method name that we do not support
 */
public class UnsupportedMethodException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public UnsupportedMethodException(String message) {
        super(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
    }
}

package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception thrown when the client sends a request for a checksum algorithm we do not support
 */
public class ChecksumAlgorithmNotSupportedException extends TusException {

	private static final long serialVersionUID = 7412336618168312418L;

	public ChecksumAlgorithmNotSupportedException(String message) {
        super(HttpServletResponse.SC_BAD_REQUEST, message);
    }
}

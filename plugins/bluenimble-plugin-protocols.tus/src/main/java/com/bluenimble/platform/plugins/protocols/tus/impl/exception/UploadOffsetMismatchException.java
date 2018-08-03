package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * If the offsets do not match, the Server MUST respond with the
 * 409 Conflict status without modifying the upload resource.
 */
public class UploadOffsetMismatchException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public UploadOffsetMismatchException(String message) {
        super(HttpServletResponse.SC_CONFLICT, message);
    }
}

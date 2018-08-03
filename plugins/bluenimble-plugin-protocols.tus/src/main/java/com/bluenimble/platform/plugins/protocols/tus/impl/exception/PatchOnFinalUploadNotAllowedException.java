package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * The Server MUST respond with the 403 Forbidden status to PATCH requests against a upload URL
 */
public class PatchOnFinalUploadNotAllowedException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public PatchOnFinalUploadNotAllowedException(String message) {
        super(HttpServletResponse.SC_FORBIDDEN, message);
    }
}

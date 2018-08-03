package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

import javax.servlet.http.HttpServletResponse;

/** Exception thrown when the given upload ID was not found
 * <p/>
 * If the resource is not found, the Server SHOULD return either the
 * 404 Not Found, 410 Gone or 403 Forbidden status without the Upload-Offset header.
 */
public class UploadNotFoundException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public UploadNotFoundException (String message) {
        super (HttpServletResponse.SC_NOT_FOUND, message);
    }

    public UploadNotFoundException (String message, Throwable th) {
        super (HttpServletResponse.SC_NOT_FOUND, message, th);
    }

}

package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

public class UploadAlreadyLockedException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public UploadAlreadyLockedException(String message) {
        // 423 is LOCKED (WebDAV rfc 4918)
        super(423, message);
    }
}

package com.bluenimble.platform.plugins.protocols.tus.impl.exception;

/**
 * Exception thrown when the client provided checksum does not match the checksum calculated by the server
 */
public class UploadChecksumMismatchException extends TusException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public UploadChecksumMismatchException(String message) {
        super(460, message);
    }
}

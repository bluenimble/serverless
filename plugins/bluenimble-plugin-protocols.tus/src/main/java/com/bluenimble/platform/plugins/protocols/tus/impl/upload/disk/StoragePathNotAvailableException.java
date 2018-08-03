package com.bluenimble.platform.plugins.protocols.tus.impl.upload.disk;

/**
 * Exception thrown when the disk storage path cannot be read or created.
 */
public class StoragePathNotAvailableException extends RuntimeException {
	
	private static final long serialVersionUID = 7412336618168312418L;

    public StoragePathNotAvailableException(String message, Throwable e) {
        super(message, e);
    }
}

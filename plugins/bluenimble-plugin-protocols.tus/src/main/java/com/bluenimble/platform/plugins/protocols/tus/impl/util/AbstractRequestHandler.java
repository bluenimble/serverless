package com.bluenimble.platform.plugins.protocols.tus.impl.util;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.RequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;

/**
 * Abstract {@link com.bluenimble.platform.plugins.protocols.tus.impl.RequestHandler} implementation that contains the common functionality
 */
public abstract class AbstractRequestHandler implements RequestHandler {

	private static final String UploadInfo = "Tus.UploadInfo";
	
    @Override
    public boolean isErrorHandler() {
        return false;
    }
    
    protected UploadInfo uploadInfo (HttpServletRequest request, UploadStorageService uploadStorageService, String ownerKey) 
        	throws IOException {
    	return uploadInfo (request, uploadStorageService, ownerKey, null);
    }
    protected UploadInfo uploadInfo (HttpServletRequest request, UploadStorageService uploadStorageService, String ownerKey, String altUri) 
    	throws IOException {
    	UploadInfo info = (UploadInfo)request.getAttribute (UploadInfo);
    	if (info != null) {
    		return info;
    	}
    	info = uploadStorageService.getUploadInfo (altUri == null ? request.getRequestURI () : altUri, ownerKey);
    	if (info != null) {
    		request.setAttribute (UploadInfo, info);
    	}
    	return info;
    }

}

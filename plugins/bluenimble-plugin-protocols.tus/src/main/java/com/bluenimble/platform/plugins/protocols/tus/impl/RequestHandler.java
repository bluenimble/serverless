package com.bluenimble.platform.plugins.protocols.tus.impl;

import java.io.IOException;

import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;

public interface RequestHandler {
	
	String UploadInfo = "Tus.UploadInfo";

    boolean supports(HttpMethod method);

    void process(HttpMethod method, TusServletRequest servletRequest,
                 TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                 String ownerKey) throws IOException, TusException;

    boolean isErrorHandler();

}

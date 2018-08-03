package com.bluenimble.platform.plugins.protocols.tus.impl.core;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.TusFileUploadService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;

/**
 * The Tus-Resumable header MUST be included in every request and response except for OPTIONS requests.
 * The value MUST be the version of the protocol used by the Client or the Server.
 */
public class CoreDefaultResponseHeadersHandler implements RequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return true;
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) {

        //Always set Tus-Resumable header
        servletResponse.setHeader(HttpHeader.TUS_RESUMABLE, TusFileUploadService.TUS_API_VERSION);
        //By default, set the Content-Length to 0
        servletResponse.setHeader(HttpHeader.CONTENT_LENGTH, "0");
    }

    @Override
    public boolean isErrorHandler() {
        return true;
    }
}

package com.bluenimble.platform.plugins.protocols.tus.impl.termination;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;

/**
 * When receiving a DELETE request for an existing upload the Server SHOULD free associated resources
 * and MUST respond with the 204 No Content status confirming that the upload was terminated. For all future requests
 * to this URL the Server SHOULD respond with the 404 Not Found or 410 Gone status.
 */
public class TerminationDeleteRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.DELETE.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException, TusException {

        UploadInfo uploadInfo = uploadInfo (servletRequest, uploadStorageService, ownerKey);

        if (uploadInfo != null) {
            uploadStorageService.terminateUpload (uploadInfo, ownerKey);
        }

        servletResponse.setStatus (HttpServletResponse.SC_NO_CONTENT);
    }

}

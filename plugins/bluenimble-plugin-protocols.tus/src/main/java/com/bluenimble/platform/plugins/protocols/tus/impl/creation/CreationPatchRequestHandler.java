package com.bluenimble.platform.plugins.protocols.tus.impl.creation;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UploadNotFoundException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Upload-Defer-Length: 1 if upload size is not known at the time. Once it is known the Client MUST set
 * the Upload-Length header in the next PATCH request. Once set the length MUST NOT be changed.
 */
public class CreationPatchRequestHandler extends AbstractRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(CreationPatchRequestHandler.class);

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException {

        UploadInfo uploadInfo = uploadInfo (servletRequest, uploadStorageService, ownerKey);

        if (uploadInfo != null && !uploadInfo.hasLength()) {
            Long uploadLength = Utils.getLongHeader(servletRequest, HttpHeader.UPLOAD_LENGTH);
            if (uploadLength != null) {
                uploadInfo.setLength(uploadLength);
                try {
                    uploadStorageService.update (uploadInfo, ownerKey);
                } catch (UploadNotFoundException e) {
                    log.error("The patch request handler could not find the upload for URL "
                            + servletRequest.getRequestURI()
                            + ". This means something is really wrong the request validators!", e);
                    servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }
}

package com.bluenimble.platform.plugins.protocols.tus.impl.creation;

import java.io.IOException;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadType;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;

/** A HEAD request can be used to retrieve the metadata that was supplied at creation.
 * <p/>
 *  If an upload contains additional metadata, responses to HEAD requests MUST include the Upload-Metadata
 *  header and its value as specified by the Client during the creation.
 * <p/>
 *  As long as the length of the upload is not known, the Server MUST set Upload-Defer-Length: 1 in
 *  all responses to HEAD requests.
 */
public class CreationHeadRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.HEAD.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException {

        UploadInfo uploadInfo = uploadInfo (servletRequest, uploadStorageService, ownerKey);

        if (uploadInfo.hasMetadata()) {
            servletResponse.setHeader(HttpHeader.UPLOAD_METADATA, uploadInfo.getEncodedMetadata());
        }

        if (!uploadInfo.hasLength() && !UploadType.CONCATENATED.equals(uploadInfo.getUploadType())) {
            servletResponse.setHeader(HttpHeader.UPLOAD_DEFER_LENGTH, "1");
        }
    }
}

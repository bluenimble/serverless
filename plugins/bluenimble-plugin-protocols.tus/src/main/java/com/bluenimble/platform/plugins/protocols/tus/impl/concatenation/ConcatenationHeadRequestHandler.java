package com.bluenimble.platform.plugins.protocols.tus.impl.concatenation;

import java.io.IOException;
import java.util.Objects;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadType;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;

/**
 * The response to a HEAD request for a upload SHOULD NOT contain the Upload-Offset header unless the
 * concatenation has been successfully finished. After successful concatenation, the Upload-Offset and
 * Upload-Length MUST be set and their values MUST be equal. The value of the Upload-Offset header before
 * concatenation is not defined for a upload.
 * <p/>
 * The response to a HEAD request for a partial upload MUST contain the Upload-Offset header. Response to HEAD
 * request against partial or upload MUST include the Upload-Concat header and its value as received in
 * the upload creation request.
 */
public class ConcatenationHeadRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.HEAD.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException, TusException {

        UploadInfo uploadInfo = uploadInfo (servletRequest, uploadStorageService, ownerKey);

        if (!UploadType.REGULAR.equals(uploadInfo.getUploadType())) {
            servletResponse.setHeader(HttpHeader.UPLOAD_CONCAT, uploadInfo.getUploadConcatHeaderValue());
        }

        if (UploadType.CONCATENATED.equals(uploadInfo.getUploadType())) {
            if (uploadInfo.isUploadInProgress()) {
                //Execute the merge function again to update our upload data
                uploadStorageService.getUploadConcatenationService().merge(uploadInfo);
            }

            if (uploadInfo.hasLength()) {
                servletResponse.setHeader(HttpHeader.UPLOAD_LENGTH, Objects.toString(uploadInfo.getLength()));
            }

            if (!uploadInfo.isUploadInProgress()) {
                servletResponse.setHeader(HttpHeader.UPLOAD_OFFSET, Objects.toString(uploadInfo.getOffset()));
            }
        }
    }
}

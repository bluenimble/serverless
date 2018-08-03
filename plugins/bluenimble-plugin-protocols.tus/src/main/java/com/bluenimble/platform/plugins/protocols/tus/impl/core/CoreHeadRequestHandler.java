package com.bluenimble.platform.plugins.protocols.tus.impl.core;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadType;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;

/** A HEAD request is used to determine the offset at which the upload should be continued.
 * <p/>
 * The Server MUST always include the Upload-Offset header in the response for a HEAD request,
 * even if the offset is 0, or the upload is already considered completed. If the size of the upload is known,
 * the Server MUST include the Upload-Length header in the response.
 * <p/>
 * The Server MUST prevent the client and/or proxies from caching the response by adding
 * the Cache-Control: no-store header to the response.
 */
public class CoreHeadRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.HEAD.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException {

        UploadInfo uploadInfo = uploadInfo (servletRequest, uploadStorageService, ownerKey);

        if (!UploadType.CONCATENATED.equals(uploadInfo.getUploadType())) {

            if (uploadInfo.hasLength()) {
                servletResponse.setHeader(HttpHeader.UPLOAD_LENGTH, Objects.toString(uploadInfo.getLength()));
            }
            servletResponse.setHeader(HttpHeader.UPLOAD_OFFSET, Objects.toString(uploadInfo.getOffset()));
        }

        servletResponse.setHeader(HttpHeader.CACHE_CONTROL, "no-store");

        servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}

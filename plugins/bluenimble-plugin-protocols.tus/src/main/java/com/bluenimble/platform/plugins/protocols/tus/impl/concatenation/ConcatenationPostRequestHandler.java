package com.bluenimble.platform.plugins.protocols.tus.impl.concatenation;

import java.io.IOException;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadType;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;
import org.apache.commons.lang3.StringUtils;

/**
 * The Server MUST acknowledge a successful upload creation with the 201 Created status.
 * The Server MUST set the Location header to the URL of the created resource. This URL MAY be absolute or relative.
 */
public class ConcatenationPostRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.POST.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException, TusException {

        //For post requests, the upload URI is part of the response
        UploadInfo uploadInfo = uploadInfo (servletRequest, uploadStorageService, ownerKey);

        if (uploadInfo != null) {

            String uploadConcatValue = servletRequest.getHeader(HttpHeader.UPLOAD_CONCAT);
            if (StringUtils.equalsIgnoreCase(uploadConcatValue, "partial")) {
                uploadInfo.setUploadType(UploadType.PARTIAL);

            } else if (StringUtils.startsWithIgnoreCase(uploadConcatValue, "final")) {
                //reset the length, just to be sure
                uploadInfo.setLength(null);
                uploadInfo.setUploadType(UploadType.CONCATENATED);
                uploadInfo.setConcatenationParts(Utils.parseConcatenationIDsFromHeader(uploadConcatValue));

                uploadStorageService.getUploadConcatenationService().merge(uploadInfo);

            } else {
                uploadInfo.setUploadType(UploadType.REGULAR);
            }

            uploadInfo.setUploadConcatHeaderValue(uploadConcatValue);

            uploadStorageService.update (uploadInfo, ownerKey);
        }
    }
}

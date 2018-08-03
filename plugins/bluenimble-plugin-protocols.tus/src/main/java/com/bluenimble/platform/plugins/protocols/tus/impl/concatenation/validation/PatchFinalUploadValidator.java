package com.bluenimble.platform.plugins.protocols.tus.impl.concatenation.validation;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.PatchOnFinalUploadNotAllowedException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadType;

/**
 * The Server MUST respond with the 403 Forbidden status to PATCH requests against a upload URL
 * and MUST NOT modify the or its partial uploads.
 */
public class PatchFinalUploadValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws IOException, TusException {

        UploadInfo uploadInfo = uploadStorageService.getUploadInfo(request.getRequestURI(), ownerKey);

        if (uploadInfo != null && UploadType.CONCATENATED.equals(uploadInfo.getUploadType())) {
            throw new PatchOnFinalUploadNotAllowedException("You cannot send a PATCH request for a "
                    + "concatenated upload URI");
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }
}

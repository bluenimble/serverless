package com.bluenimble.platform.plugins.protocols.tus.impl.core.validation;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UploadOffsetMismatchException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;
import org.apache.commons.lang3.StringUtils;

/**
 * The Upload-Offset header’s value MUST be equal to the current offset of the resource.
 * If the offsets do not match, the Server MUST respond with the
 * 409 Conflict status without modifying the upload resource.
 */
public class UploadOffsetValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws IOException, TusException {

        String uploadOffset = Utils.getHeader(request, HttpHeader.UPLOAD_OFFSET);

        UploadInfo uploadInfo = uploadStorageService.getUploadInfo(request.getRequestURI(), ownerKey);

        if (uploadInfo != null) {
            String expectedOffset = Objects.toString(uploadInfo.getOffset());
            if (!StringUtils.equals(expectedOffset, uploadOffset)) {
                throw new UploadOffsetMismatchException("The Upload-Offset was "
                        + StringUtils.trimToNull(uploadOffset) + " but expected " + expectedOffset);
            }
        }

    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }

}

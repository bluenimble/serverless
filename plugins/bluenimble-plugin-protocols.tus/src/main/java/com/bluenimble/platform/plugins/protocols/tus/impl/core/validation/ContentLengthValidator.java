package com.bluenimble.platform.plugins.protocols.tus.impl.core.validation;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.InvalidContentLengthException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;

/**
 * Validate that the given upload length in combination with the bytes we already received,
 * does not exceed the declared initial length on upload creation.
 */
public class ContentLengthValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws TusException, IOException {

        Long contentLength = Utils.getLongHeader(request, HttpHeader.CONTENT_LENGTH);

        UploadInfo uploadInfo = uploadStorageService.getUploadInfo(request.getRequestURI(), ownerKey);

        if (contentLength != null
                && uploadInfo != null
                && uploadInfo.hasLength()
                && (uploadInfo.getOffset() + contentLength > uploadInfo.getLength())) {

            throw new InvalidContentLengthException("The " + HttpHeader.CONTENT_LENGTH + " value " + contentLength
                    + " in combination with the current offset " + uploadInfo.getOffset()
                    + " exceeds the declared upload length " + uploadInfo.getLength());
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }

}

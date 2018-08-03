package com.bluenimble.platform.plugins.protocols.tus.impl.creation.validation;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.MaxUploadLengthExceededException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;

/**
 * If the length of the upload exceeds the maximum, which MAY be specified using the Tus-Max-Size header,
 * the Server MUST respond with the 413 Request Entity Too Large status.
 */
public class UploadLengthValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws TusException {

        Long uploadLength = Utils.getLongHeader(request, HttpHeader.UPLOAD_LENGTH);
        if (uploadLength != null
                && uploadStorageService.getMaxUploadSize() > 0
                && uploadLength > uploadStorageService.getMaxUploadSize()) {

            throw new MaxUploadLengthExceededException("Upload requests can have a maximum size of "
                    + uploadStorageService.getMaxUploadSize());
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.POST.equals(method);
    }
}

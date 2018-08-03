package com.bluenimble.platform.plugins.protocols.tus.impl.concatenation.validation;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UploadLengthNotAllowedOnConcatenationException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import org.apache.commons.lang3.StringUtils;

/**
 * The Client MUST NOT include the Upload-Length header in the upload creation.
 */
public class NoUploadLengthOnFinalValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws IOException, TusException {

        String uploadConcatValue = request.getHeader(HttpHeader.UPLOAD_CONCAT);

        if (StringUtils.startsWithIgnoreCase(uploadConcatValue, "final")
                && StringUtils.isNotBlank(request.getHeader(HttpHeader.UPLOAD_LENGTH))) {

            throw new UploadLengthNotAllowedOnConcatenationException(
                    "The upload length of a concatenated upload cannot be set");
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.POST.equals(method);
    }
}

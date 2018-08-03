package com.bluenimble.platform.plugins.protocols.tus.impl.concatenation.validation;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.InvalidPartialUploadIdException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;
import org.apache.commons.lang3.StringUtils;

/**
 * Validate that the IDs specified in the Upload-Concat header map to an existing upload
 */
public class PartialUploadsExistValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws IOException, TusException {

        String uploadConcatValue = request.getHeader(HttpHeader.UPLOAD_CONCAT);

        if (StringUtils.startsWithIgnoreCase(uploadConcatValue, "final")) {

            for (String uploadUri : Utils.parseConcatenationIDsFromHeader(uploadConcatValue)) {

                UploadInfo uploadInfo = uploadStorageService.getUploadInfo(uploadUri, ownerKey);
                if (uploadInfo == null) {
                    throw new InvalidPartialUploadIdException("The URI " + uploadUri
                            + " in Upload-Concat header does not match an existing upload");
                }
            }
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.POST.equals(method);
    }

}

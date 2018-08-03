package com.bluenimble.platform.plugins.protocols.tus.impl.core.validation;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.InvalidContentTypeException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;

/**
 * All PATCH requests MUST use Content-Type: application/offset+octet-stream.
 */
public class ContentTypeValidator implements RequestValidator {

    static final String APPLICATION_OFFSET_OCTET_STREAM = "application/offset+octet-stream";

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey) throws TusException {

        String contentType = Utils.getHeader(request, HttpHeader.CONTENT_TYPE);
        if (!APPLICATION_OFFSET_OCTET_STREAM.equals(contentType)) {
            throw new InvalidContentTypeException("The " + HttpHeader.CONTENT_TYPE + " header must contain value "
                    + APPLICATION_OFFSET_OCTET_STREAM);
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }

}

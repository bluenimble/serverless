package com.bluenimble.platform.plugins.protocols.tus.impl.creation.validation;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.InvalidContentLengthException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;

/**
 * An empty POST request is used to create a new upload resource.
 */
public class PostEmptyRequestValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws TusException {

        Long contentLength = Utils.getLongHeader(request, HttpHeader.CONTENT_LENGTH);
        if (contentLength != null && contentLength > 0) {
            throw new InvalidContentLengthException("A POST request should have a Content-Length header with value "
                    + "0 and no content");
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.POST.equals(method);
    }
}

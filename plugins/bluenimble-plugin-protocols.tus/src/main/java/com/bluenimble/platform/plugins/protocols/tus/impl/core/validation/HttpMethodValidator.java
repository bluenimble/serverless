package com.bluenimble.platform.plugins.protocols.tus.impl.core.validation;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UnsupportedMethodException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;

/**
 * Class to validate if the current HTTP method is valid
 */
public class HttpMethodValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey) throws TusException {

        if (method == null) {
            throw new UnsupportedMethodException("The HTTP method " + request.getMethod() + " is not supported");
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return true;
    }

}

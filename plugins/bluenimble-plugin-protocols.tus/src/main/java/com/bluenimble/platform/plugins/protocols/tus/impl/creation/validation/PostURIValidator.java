package com.bluenimble.platform.plugins.protocols.tus.impl.creation.validation;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;

/**
 * The Client MUST send a POST request against a known upload creation URL to request a new upload resource.
 */
public class PostURIValidator implements RequestValidator {

    @Override
    public void validate (HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws TusException {
    	
    	/*
        if (!StringUtils.equals(request.getRequestURI(), uploadStorageService.getUploadURI())) {
            throw new PostOnInvalidRequestURIException("POST requests have to be send to "
                    + uploadStorageService.getUploadURI());
        }
        */
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.POST.equals(method);
    }

}

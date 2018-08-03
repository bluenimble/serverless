package com.bluenimble.platform.plugins.protocols.tus.impl.core.validation;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.TusFileUploadService;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.InvalidTusResumableException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;
import org.apache.commons.lang3.StringUtils;

/** Class that will validate if the tus version in the request corresponds to our implementation version
 * <p/>
 * The Tus-Resumable header MUST be included in every request and response except for OPTIONS requests.
 * The value MUST be the version of the protocol used by the Client or the Server.
 * If the the version specified by the Client is not supported by the Server, it MUST respond with the
 * 412 Precondition Failed status and MUST include the Tus-Version header into the response.
 * In addition, the Server MUST NOT process the request.
 * <p/>
 * (https://tus.io/protocols/resumable-upload.html#tus-resumable)
 */
public class TusResumableValidator implements RequestValidator {

    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws TusException {

        String requestVersion = Utils.getHeader(request, HttpHeader.TUS_RESUMABLE);
        if (!StringUtils.equals(requestVersion, TusFileUploadService.TUS_API_VERSION)) {
            throw new InvalidTusResumableException("This server does not support tus protocol version "
                    + requestVersion);
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return !HttpMethod.OPTIONS.equals(method) && !HttpMethod.GET.equals(method);
    }
}

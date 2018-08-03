package com.bluenimble.platform.plugins.protocols.tus.impl.core;

import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.TusFileUploadService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;

/**
 * An OPTIONS request MAY be used to gather information about the Server’s current configuration. A successful
 * response indicated by the 204 No Content or 200 OK status MUST contain the Tus-Version header. It MAY include
 * the Tus-Extension and Tus-Max-Size headers.
 */
public class CoreOptionsRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.OPTIONS.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) {

        if (uploadStorageService.getMaxUploadSize() > 0) {
            servletResponse.setHeader(HttpHeader.TUS_MAX_SIZE,
                    Objects.toString(uploadStorageService.getMaxUploadSize()));
        }

        servletResponse.setHeader(HttpHeader.TUS_VERSION, TusFileUploadService.TUS_API_VERSION);

        servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}

package com.bluenimble.platform.plugins.protocols.tus.impl.util;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract request handler to add tus extension values to the correct header
 */
public abstract class AbstractExtensionRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.OPTIONS.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) {

        StringBuilder extensionBuilder = new StringBuilder(StringUtils.trimToEmpty(
                servletResponse.getHeader(HttpHeader.TUS_EXTENSION)));

        appendExtensions(extensionBuilder);

        servletResponse.setHeader(HttpHeader.TUS_EXTENSION, extensionBuilder.toString());
    }

    protected abstract void appendExtensions(StringBuilder extensionBuilder);

    protected void addExtension(StringBuilder stringBuilder, String extension) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(",");
        }
        stringBuilder.append(extension);
    }
}

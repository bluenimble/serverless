package com.bluenimble.platform.plugins.protocols.tus.impl.download;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractTusExtension;

/**
 * Some Tus clients also send GET request to retrieve the uploaded content. We consider this
 * as an unofficial extension.
 */
public class DownloadExtension extends AbstractTusExtension {

    @Override
    public String getName() {
        return "download";
    }

    @Override
    public Collection<HttpMethod> getMinimalSupportedHttpMethods() {
        return Arrays.asList(HttpMethod.OPTIONS, HttpMethod.GET);
    }

    @Override
    protected void initValidators(List<RequestValidator> requestValidators) {
        //All validation is all read done by the Core protocol
    }

    @Override
    protected void initRequestHandlers(List<RequestHandler> requestHandlers) {
        requestHandlers.add(new DownloadGetRequestHandler());
        requestHandlers.add(new DownloadOptionsRequestHandler());
    }
}

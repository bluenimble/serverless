package com.bluenimble.platform.plugins.protocols.tus.impl.checksum;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.checksum.validation.ChecksumAlgorithmValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractTusExtension;

/**
 * The Client and the Server MAY implement and use this extension to verify data integrity of each PATCH request.
 * If supported, the Server MUST add checksum to the Tus-Extension header.
 */
public class ChecksumExtension extends AbstractTusExtension {

    @Override
    public String getName() {
        return "checksum";
    }

    @Override
    public Collection<HttpMethod> getMinimalSupportedHttpMethods() {
        return Arrays.asList(HttpMethod.OPTIONS, HttpMethod.PATCH);
    }

    @Override
    protected void initValidators(List<RequestValidator> requestValidators) {
        requestValidators.add(new ChecksumAlgorithmValidator());
    }

    @Override
    protected void initRequestHandlers(List<RequestHandler> requestHandlers) {
        requestHandlers.add(new ChecksumOptionsRequestHandler());
        requestHandlers.add(new ChecksumPatchRequestHandler());
    }
}

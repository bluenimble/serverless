package com.bluenimble.platform.plugins.protocols.tus.impl.concatenation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.concatenation.validation.NoUploadLengthOnFinalValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.concatenation.validation.PartialUploadsExistValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.concatenation.validation.PatchFinalUploadValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractTusExtension;

/**
 * This extension can be used to concatenate multiple uploads into a single one enabling Clients
 * to perform parallel uploads and to upload non-contiguous chunks.
 * If the Server supports this extension, it MUST add concatenation to the Tus-Extension header.
 */
public class ConcatenationExtension extends AbstractTusExtension {

    @Override
    public String getName() {
        return "concatenation";
    }

    @Override
    public Collection<HttpMethod> getMinimalSupportedHttpMethods() {
        return Arrays.asList(HttpMethod.OPTIONS, HttpMethod.POST, HttpMethod.PATCH, HttpMethod.HEAD);
    }

    @Override
    protected void initValidators(List<RequestValidator> requestValidators) {
        requestValidators.add(new PatchFinalUploadValidator());
        requestValidators.add(new NoUploadLengthOnFinalValidator());
        requestValidators.add(new PartialUploadsExistValidator());
    }

    @Override
    protected void initRequestHandlers(List<RequestHandler> requestHandlers) {
        requestHandlers.add(new ConcatenationOptionsRequestHandler());
        requestHandlers.add(new ConcatenationPostRequestHandler());
        requestHandlers.add(new ConcatenationHeadRequestHandler());
    }
}

package com.bluenimble.platform.plugins.protocols.tus.impl.creation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.creation.validation.PostEmptyRequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.creation.validation.PostURIValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.creation.validation.UploadDeferLengthValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.creation.validation.UploadLengthValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractTusExtension;

/**
 * The Client and the Server SHOULD implement the upload creation extension. If the Server supports this extension.
 */
public class CreationExtension extends AbstractTusExtension {

    @Override
    public String getName() {
        return "creation";
    }

    @Override
    public Collection<HttpMethod> getMinimalSupportedHttpMethods() {
        return Arrays.asList(HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.PATCH, HttpMethod.POST);
    }

    @Override
    protected void initValidators(List<RequestValidator> requestValidators) {
        requestValidators.add(new PostURIValidator());
        requestValidators.add(new PostEmptyRequestValidator());
        requestValidators.add(new UploadDeferLengthValidator());
        requestValidators.add(new UploadLengthValidator());
    }

    @Override
    protected void initRequestHandlers(List<RequestHandler> requestHandlers) {
        requestHandlers.add(new CreationHeadRequestHandler());
        requestHandlers.add(new CreationPatchRequestHandler());
        requestHandlers.add(new CreationPostRequestHandler());
        requestHandlers.add(new CreationOptionsRequestHandler());
    }
}

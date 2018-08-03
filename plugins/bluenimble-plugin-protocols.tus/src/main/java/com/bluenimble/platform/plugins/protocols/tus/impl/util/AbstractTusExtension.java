package com.bluenimble.platform.plugins.protocols.tus.impl.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.TusExtension;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;

public abstract class AbstractTusExtension implements TusExtension {

    private List<RequestValidator> requestValidators = new LinkedList<>();
    private List<RequestHandler> requestHandlers = new LinkedList<>();

    public AbstractTusExtension() {
        initValidators(requestValidators);
        initRequestHandlers(requestHandlers);
    }

    protected abstract void initValidators(List<RequestValidator> requestValidators);

    protected abstract void initRequestHandlers(List<RequestHandler> requestHandlers);

    @Override
    public void validate(HttpMethod method, HttpServletRequest servletRequest,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws TusException, IOException {

        for (RequestValidator requestValidator : requestValidators) {
            if (requestValidator.supports(method)) {
                requestValidator.validate(method, servletRequest, uploadStorageService, ownerKey);
            }
        }
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException, TusException {

        for (RequestHandler requestHandler : requestHandlers) {
            if (requestHandler.supports(method)) {
                requestHandler.process(method, servletRequest, servletResponse, uploadStorageService, ownerKey);
            }
        }
    }

    @Override
    public void handleError(HttpMethod method, TusServletRequest request, TusServletResponse response,
                            UploadStorageService uploadStorageService, String ownerKey)
            throws IOException, TusException {

        for (RequestHandler requestHandler : requestHandlers) {
            if (requestHandler.supports(method) && requestHandler.isErrorHandler()) {
                requestHandler.process (method, request, response, uploadStorageService, ownerKey);
            }
        }
    }
}

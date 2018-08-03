package com.bluenimble.platform.plugins.protocols.tus.impl.download;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UploadInProgressException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;

/**
 * Send the uploaded bytes of finished uploads
 */
public class DownloadGetRequestHandler extends AbstractRequestHandler {

    private static final String CONTENT_DISPOSITION_FORMAT = "attachment;filename=\"%s\"";

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.GET.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException, TusException {

        UploadInfo info = uploadStorageService.getUploadInfo(servletRequest.getRequestURI(), ownerKey);
        if (info == null || info.isUploadInProgress()) {
            throw new UploadInProgressException("Upload " + servletRequest.getRequestURI() + " is still in progress "
                    + "and cannot be downloaded yet");
        } else {

            servletResponse.setHeader(HttpHeader.CONTENT_LENGTH, Objects.toString(info.getLength()));

            servletResponse.setHeader(HttpHeader.CONTENT_DISPOSITION,
                    String.format(CONTENT_DISPOSITION_FORMAT, info.getFileName()));

            servletResponse.setHeader(HttpHeader.CONTENT_TYPE, info.getFileMimeType());

            if (info.hasMetadata()) {
                servletResponse.setHeader(HttpHeader.UPLOAD_METADATA, info.getEncodedMetadata());
            }

            uploadStorageService.copyUploadTo (info, ownerKey, servletResponse.getOutputStream());
        }

        servletResponse.setStatus(HttpServletResponse.SC_OK);
    }
}

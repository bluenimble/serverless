package com.bluenimble.platform.plugins.protocols.tus.impl.checksum;

import static com.bluenimble.platform.plugins.protocols.tus.impl.checksum.ChecksumAlgorithm.CHECKSUM_VALUE_SEPARATOR;

import java.io.IOException;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.checksum.validation.ChecksumAlgorithmValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UploadChecksumMismatchException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractRequestHandler;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletRequest;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.TusServletResponse;
import org.apache.commons.lang3.StringUtils;

public class ChecksumPatchRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException, TusException {

        String uploadChecksumHeader = servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM);

        if (servletRequest.hasCalculatedChecksum() && StringUtils.isNotBlank(uploadChecksumHeader)) {

            //The Upload-Checksum header can be a trailing header which is only present after reading the full content.
            //Therefor we need to revalidate that header here
            new ChecksumAlgorithmValidator().validate(method, servletRequest, uploadStorageService, ownerKey);

            //Everything is valid, check if the checksum matches
            String expectedValue = StringUtils.substringAfter(uploadChecksumHeader, CHECKSUM_VALUE_SEPARATOR);

            ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.forUploadChecksumHeader(uploadChecksumHeader);
            String calculatedValue = servletRequest.getCalculatedChecksum(checksumAlgorithm);

            if (!StringUtils.equals(expectedValue, calculatedValue)) {
                //throw an exception if the checksum is invalid. This will also trigger the removal of any
                //bytes that were already saved
                throw new UploadChecksumMismatchException("Expected checksum " + expectedValue
                        + " but was " + calculatedValue
                        + " with algorithm " + checksumAlgorithm);
            }
        }
    }

}

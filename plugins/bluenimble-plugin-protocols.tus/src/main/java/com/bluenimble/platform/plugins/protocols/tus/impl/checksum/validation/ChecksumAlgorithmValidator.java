package com.bluenimble.platform.plugins.protocols.tus.impl.checksum.validation;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.bluenimble.platform.plugins.protocols.tus.impl.HttpHeader;
import com.bluenimble.platform.plugins.protocols.tus.impl.HttpMethod;
import com.bluenimble.platform.plugins.protocols.tus.impl.RequestValidator;
import com.bluenimble.platform.plugins.protocols.tus.impl.checksum.ChecksumAlgorithm;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.ChecksumAlgorithmNotSupportedException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import org.apache.commons.lang3.StringUtils;

/**
 * The Server MAY respond with one of the following status code: 400 Bad Request
 * if the checksum algorithm is not supported by the server
 */
public class ChecksumAlgorithmValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws TusException, IOException {

        String uploadChecksum = request.getHeader(HttpHeader.UPLOAD_CHECKSUM);

        //If the client provided a checksum header, check that we support the algorithm
        if (StringUtils.isNotBlank(uploadChecksum)
                && ChecksumAlgorithm.forUploadChecksumHeader(uploadChecksum) == null) {

            throw new ChecksumAlgorithmNotSupportedException("The " + HttpHeader.UPLOAD_CHECKSUM + " header value "
                    + uploadChecksum + " is not supported");

        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }
}

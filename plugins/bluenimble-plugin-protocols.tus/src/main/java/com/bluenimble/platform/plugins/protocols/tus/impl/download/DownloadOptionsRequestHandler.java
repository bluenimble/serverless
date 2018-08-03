package com.bluenimble.platform.plugins.protocols.tus.impl.download;

import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractExtensionRequestHandler;

/**
 * Add our download extension the Tus-Extension header
 */
public class DownloadOptionsRequestHandler extends AbstractExtensionRequestHandler {

    @Override
    protected void appendExtensions(StringBuilder extensionBuilder) {
        addExtension(extensionBuilder, "download");
    }

}

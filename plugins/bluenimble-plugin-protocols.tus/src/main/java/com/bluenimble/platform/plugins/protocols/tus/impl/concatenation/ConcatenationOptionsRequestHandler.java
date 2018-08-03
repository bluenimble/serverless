package com.bluenimble.platform.plugins.protocols.tus.impl.concatenation;

import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractExtensionRequestHandler;

/**
 * If the Server supports this extension, it MUST add concatenation to the Tus-Extension header.
 * The Client MAY send the concatenation request while the partial uploads are still in progress.
 * This feature MUST be explicitly announced by the Server by adding concatenation-unfinished to
 * the Tus-Extension header.
 */
public class ConcatenationOptionsRequestHandler extends AbstractExtensionRequestHandler {

    @Override
    protected void appendExtensions(StringBuilder extensionBuilder) {
        addExtension(extensionBuilder, "concatenation");
        addExtension(extensionBuilder, "concatenation-unfinished");
    }

}

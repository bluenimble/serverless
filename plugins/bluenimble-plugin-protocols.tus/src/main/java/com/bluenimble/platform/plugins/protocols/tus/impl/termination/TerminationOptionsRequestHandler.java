package com.bluenimble.platform.plugins.protocols.tus.impl.termination;

import com.bluenimble.platform.plugins.protocols.tus.impl.util.AbstractExtensionRequestHandler;

/**
 * Add our download extension the Tus-Extension header
 */
public class TerminationOptionsRequestHandler extends AbstractExtensionRequestHandler {

    @Override
    protected void appendExtensions(StringBuilder extensionBuilder) {
        addExtension(extensionBuilder, "termination");
    }

}

package com.bluenimble.platform.remote.impls.serializers;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class InvalidXmlCharacterFilter extends FilterReader {

    protected InvalidXmlCharacterFilter (Reader in) {
        super (in);
    }

    @Override
    public int read (char[] cbuf, int off, int len) throws IOException {
        int read = super.read(cbuf, off, len);
        if (read == -1) return read;

        for (int i = off; i < off + read; i++) {
            if (!XmlChar.isValid(cbuf[i])) cbuf[i] = ' ';
        }
        return read;
    }
}


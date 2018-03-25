package com.bluenimble.platform.api.tracing.impls;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class DefaultLoggingHandler extends Handler {

	@Override
	public void publish (LogRecord record) {
		
	}

	@Override
	public void flush () {
		// IGNORE
	}

	@Override
	public void close () throws SecurityException {
		// IGNORE
	}

}

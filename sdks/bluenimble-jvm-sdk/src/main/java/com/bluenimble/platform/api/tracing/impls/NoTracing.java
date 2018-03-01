package com.bluenimble.platform.api.tracing.impls;

import com.bluenimble.platform.Traceable;
import com.bluenimble.platform.api.tracing.Tracer;

public class NoTracing implements Tracer {

	private static final long serialVersionUID = -8545595874492236766L;
	
	public static final Tracer Instance = new NoTracing ();

	@Override
	public void log (Level level, Object o, Throwable th) {
	}

	@Override
	public void log (Level level, Object o, Object... args) {
	}

	@Override
	public void onInstall (Traceable traceable) {
		
	}
	@Override
	public void onShutdown (Traceable traceable) {
		
	}

}

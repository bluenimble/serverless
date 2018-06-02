package com.bluenimble.platform.plugins.inbound.http.jetty;

import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.Logger;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.api.tracing.Tracer.Level;

public class JettyLogger extends AbstractLogger {

	private static final String MsgParam 	= "{}";
	private static final String MsgToken 	= "%s";
	
	private String name;
	private Tracer tracer;
	
	public JettyLogger (JettyPlugin plugin) {
		this.name 	= plugin.getNamespace ();
		this.tracer = plugin.tracer ();
	}
	
	@Override
	public String getName () {
		return name;
	}

	@Override
	public void debug (Throwable th) {
		tracer.log (Level.Debug, Lang.BLANK, th);
	}

	@Override
	public void debug (String message, Object... args) {
		if (!tracer.isEnabled (Level.Debug)) {
			return;
		}
		tracer.log (Level.Debug, message (message, args));
	}

	@Override
	public void debug (String message, Throwable th) {
		if (!tracer.isEnabled (Level.Debug)) {
			return;
		}
		tracer.log (Level.Debug, message, th);
	}

	@Override
	public void info (Throwable th) {
		if (!tracer.isEnabled (Level.Info)) {
			return;
		}
		tracer.log (Level.Info, Lang.BLANK, th);
	}

	@Override
	public void info (String message, Object... args) {
		if (!tracer.isEnabled (Level.Info)) {
			return;
		}
		tracer.log (Level.Info, message (message, args));
	}

	@Override
	public void info (String message, Throwable th) {
		if (!tracer.isEnabled (Level.Info)) {
			return;
		}
		tracer.log (Level.Info, message, th);
	}

	@Override
	public void warn (Throwable th) {
		if (!tracer.isEnabled (Level.Warning)) {
			return;
		}
		tracer.log (Level.Info, Lang.BLANK, th);
	}

	@Override
	public void warn (String message, Object... args) {
		if (!tracer.isEnabled (Level.Warning)) {
			return;
		}
		tracer.log (Level.Info, message (message, args));
	}

	@Override
	public void warn (String message, Throwable th) {
		if (!tracer.isEnabled (Level.Warning)) {
			return;
		}
		tracer.log (Level.Info, message, th);
	}

	@Override
	protected Logger newLogger (String arg0) {
		return this;
	}

	@Override
	public void ignore (Throwable th) {
		tracer.log (Level.Debug, Lang.BLANK, th);
	}

	@Override
	public boolean isDebugEnabled () {
		return tracer.isEnabled (Level.Debug);
	}

	@Override
	public void setDebugEnabled (boolean enabled) {
		// ignore
	}
	
	private String message (String message, Object... args) {
		return String.format (message.replace (MsgParam, MsgToken), args);
	}

}

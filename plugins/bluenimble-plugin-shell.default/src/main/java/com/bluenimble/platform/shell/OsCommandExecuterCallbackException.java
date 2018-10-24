package com.bluenimble.platform.shell;

public class OsCommandExecuterCallbackException extends Exception {

	private static final long serialVersionUID = 445615871551158801L;

	public OsCommandExecuterCallbackException () {
		super ();
	}

	public OsCommandExecuterCallbackException (String message) {
		super (message);
	}

	public OsCommandExecuterCallbackException (Throwable th) {
		super (th);
	}

	public OsCommandExecuterCallbackException (String message, Throwable th) {
		super (message, th);
	}

}

package com.bluenimble.platform.shell;

public class OsCommandExecuterException extends Exception {

	private static final long serialVersionUID = 445615871551158801L;

	private int exitValue;
	
	public OsCommandExecuterException (int exitValue) {
		super ();
		this.exitValue = exitValue;
	}

	public OsCommandExecuterException (int exitValue, String message) {
		super (message);
	}

	public OsCommandExecuterException (Throwable th) {
		super (th);
	}

	public OsCommandExecuterException (String message, Throwable th) {
		super (message, th);
	}

	public int getExitValue () {
		return exitValue;
	}

}

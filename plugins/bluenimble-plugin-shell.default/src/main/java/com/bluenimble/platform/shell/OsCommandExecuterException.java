package com.bluenimble.platform.shell;

public class OsCommandExecuterException extends Exception {

	private static final long serialVersionUID = 445615871551158801L;
	
	public static final int OtherError = 10000;

	private int exitValue;
	
	public OsCommandExecuterException (int exitValue) {
		super ();
		this.exitValue = exitValue;
	}

	public OsCommandExecuterException (int exitValue, String message) {
		super (message);
		this.exitValue = exitValue;
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

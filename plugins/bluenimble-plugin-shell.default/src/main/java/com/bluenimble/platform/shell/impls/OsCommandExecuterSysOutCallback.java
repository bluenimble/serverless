package com.bluenimble.platform.shell.impls;

import java.io.InputStream;

import com.bluenimble.platform.shell.OsCommandExecuterCallback;
import com.bluenimble.platform.shell.OsCommandExecuterCallbackException;
import com.bluenimble.platform.shell.OsProcessHandle;

public class OsCommandExecuterSysOutCallback implements OsCommandExecuterCallback {

	private static final long serialVersionUID = -6836311635021572281L;

	public void finish (int exitValue, String success, OsProcessHandle handle) throws OsCommandExecuterCallbackException {
		System.out.println ("Exit Code: " + exitValue);
		System.out.println (success);
		handle.destroy ();
	}
	
	public boolean isStreaming () {
		return false;
	}

	public void intercept (InputStream stream, OsProcessHandle handle)
			throws OsCommandExecuterCallbackException {
		throw new OsCommandExecuterCallbackException ("intercept not implemented by " + OsCommandExecuterSysOutCallback.class.getName ());
	}
	
}

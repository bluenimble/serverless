package com.bluenimble.platform.shell.impls;

import java.io.InputStream;

import com.bluenimble.platform.shell.OsCommandExecuterCallback;
import com.bluenimble.platform.shell.OsCommandExecuterCallbackException;
import com.bluenimble.platform.shell.OsProcessHandle;

public abstract class OsCommandExecuterStringCallback implements OsCommandExecuterCallback {

	private static final long serialVersionUID = -6836311635021572281L;

	public boolean isStreaming () {
		return false;
	}

	public void intercept (InputStream stream, OsProcessHandle handle)
			throws OsCommandExecuterCallbackException {
		throw new OsCommandExecuterCallbackException ("intercept not implemented by " + OsCommandExecuterStringCallback.class.getName ());
	}
	
}

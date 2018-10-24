package com.bluenimble.platform.shell.impls;

import com.bluenimble.platform.shell.OsCommandExecuterCallback;
import com.bluenimble.platform.shell.OsCommandExecuterCallbackException;
import com.bluenimble.platform.shell.OsProcessHandle;

public abstract class OsCommandExecuterStreamCallback implements OsCommandExecuterCallback {
	
	private static final long serialVersionUID = 3653392120013112379L;
	
	public boolean	isStreaming () {
		return true;
	}
	
	public void finish (String response, OsProcessHandle handle) throws OsCommandExecuterCallbackException {
		throw new OsCommandExecuterCallbackException ("finish not implemented by " + OsCommandExecuterStreamCallback.class.getName ());
	}
	
}

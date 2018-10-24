package com.bluenimble.platform.shell;

import java.io.InputStream;
import java.io.Serializable;

public interface OsCommandExecuterCallback extends Serializable {
	
	boolean	isStreaming ();
	void 	finish 		(int exitValue, String response, OsProcessHandle handle) throws OsCommandExecuterCallbackException;
	void 	intercept 	(InputStream stream, OsProcessHandle handle) throws OsCommandExecuterCallbackException;
	
}

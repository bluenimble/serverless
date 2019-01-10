package com.bluenimble.platform.icli.mgm.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.bluenimble.platform.shell.OsCommandExecuter;
import com.bluenimble.platform.shell.OsCommandExecuterCallbackException;
import com.bluenimble.platform.shell.OsCommandExecuterException;
import com.bluenimble.platform.shell.OsProcessHandle;
import com.bluenimble.platform.shell.impls.DefaultOsCommandExecuter;
import com.bluenimble.platform.shell.impls.OsCommandExecuterStreamCallback;

public class OsCommander {
	
	private static final OsCommandExecuter OsCommandExecuter = new DefaultOsCommandExecuter ();

	public static void execute (JsTool tool, File baseDirectory, String command, OutputStream output) throws OsCommandExecuterException {
		
		OsCommandExecuter.execute (command, null, baseDirectory, new OsCommandExecuterStreamCallback () {
			private static final long serialVersionUID = -575527900617284872L;

			public void finish (int exitValue, String success, OsProcessHandle handle) throws OsCommandExecuterCallbackException {
				handle.destroy ();
			}

			@Override
			public void intercept (InputStream stream, OsProcessHandle handle)
					throws OsCommandExecuterCallbackException {
				try {
					tool.proxy ().drain (stream);
				} catch (IOException e) {
					throw new OsCommandExecuterCallbackException (e.getMessage (), e);
				}
			}
			
		});
	}
	
}

package com.bluenimble.platform.shell;

import com.bluenimble.platform.shell.impls.OsCommandExecuterNoCallback;
import com.bluenimble.platform.shell.impls.OsCommandExecuterSysOutCallback;

public interface OsCommandExecuterDefaultCallbacks {
	
	OsCommandExecuterCallback NoCallback 	= new OsCommandExecuterNoCallback ();
	OsCommandExecuterCallback SystemOut 	= new OsCommandExecuterSysOutCallback ();
	
}

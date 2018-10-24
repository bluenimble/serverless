package com.bluenimble.platform.shell;

import java.io.File;
import java.io.Serializable;

public interface OsCommandExecuter extends Serializable {
	void execute (String command, OsCommandExecuterCallback callback) throws OsCommandExecuterException;
	void execute (String command, String[] env, File baseDir, OsCommandExecuterCallback callback) throws OsCommandExecuterException;
}

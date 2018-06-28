package com.bluenimble.platform.icli.mgm.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.PumpStreamHandler;

import com.bluenimble.platform.cli.command.CommandExecutionException;

public class OsCommander {

	public static int execute (File workingDir, String command, OutputStream output) throws CommandExecutionException {
		CommandLine cmdLine = CommandLine.parse (command);
		DefaultExecutor executor = new DefaultExecutor ();
		executor.setWorkingDirectory (workingDir);
		
		ExecuteStreamHandler handler = null;
		
		if (output != null) {
			handler = new PumpStreamHandler (output);
		}
		
		if (handler != null) {
			executor.setStreamHandler (handler);
		}

		int exitValue = 0;
		try {
			exitValue = executor.execute (cmdLine);
		} catch (IOException ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		if (exitValue != 0) {
			throw new CommandExecutionException (command + " error : Exit value is " + exitValue);
		}
		return exitValue;
	}
	
}

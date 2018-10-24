package com.bluenimble.platform.shell.impls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.shell.OsCommandExecuter;
import com.bluenimble.platform.shell.OsCommandExecuterCallback;
import com.bluenimble.platform.shell.OsCommandExecuterCallbackException;
import com.bluenimble.platform.shell.OsCommandExecuterDefaultCallbacks;
import com.bluenimble.platform.shell.OsCommandExecuterException;
import com.bluenimble.platform.shell.OsProcessHandle;

public class DefaultOsCommandExecuter implements OsCommandExecuter {

	private static final long serialVersionUID = -6491340189778004033L;

	private static String OsName = System.getProperty ("os.name").toLowerCase();
	private static final int BUFFER = 1024 * 4;
	
	private Set<Integer> successCodes;
	
	public DefaultOsCommandExecuter () {
		this (null);
	}
	
	public DefaultOsCommandExecuter (Set<Integer> successCodes) {
		if (successCodes == null) {
			successCodes = new HashSet<Integer> ();
			successCodes.add (0);
		}
		this.successCodes = successCodes;
	}
	
	@Override
	public void execute (String command, OsCommandExecuterCallback callback)
			throws OsCommandExecuterException {
		execute (command, null, null, callback);
	}

	private OsCommandExecuterException fetchError (int exitValue, Process process, OsCommandExecuterCallback callback) {
		InputStream error = process.getErrorStream ();
		OsCommandExecuterException ex = null;
		try {
			if (error == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder ();
			int size = 0;
			try {
				size = copy (error, sb);
			} catch (IOException e) {
				return new OsCommandExecuterException (e);
			}
			if (size <= 0) {
				sb = null;
				return null;
			}
			ex = new OsCommandExecuterException (exitValue, sb.toString ());
			sb.setLength (0);
			sb = null;
		} finally {
			if (!callback.isStreaming ()) {
				process.destroy ();
			}
		}
		return ex;
	}

	private void fetchSuccess (int exitValue, Process process, OsCommandExecuterCallback callback) throws OsCommandExecuterException {
		InputStream info = process.getInputStream ();
		
		if (callback.isStreaming ()) {
			try {
				callback.intercept (info, new DefaultCommandHandle (process));
			} catch (OsCommandExecuterCallbackException e) {
				throw new OsCommandExecuterException (e);
			} 
		} else {
			StringBuilder sb = new StringBuilder();
			try {
				copy (info, sb);
			} catch (IOException e) {
				throw new OsCommandExecuterException(e);
			}
			String success = sb.toString ();
			sb.setLength (0);
			sb = null;

			try {
				callback.finish (exitValue, success, new DefaultCommandHandle (process));
			} catch (OsCommandExecuterCallbackException e) {
				throw new OsCommandExecuterException (e);
			} 
			
		}
	}

	public static int copy (InputStream input, StringBuilder sb)
			throws IOException {
		if (input == null) {
			return 0;
		}
		byte[] buffer = new byte[BUFFER];
		int count = 0;
		int n = 0;
		while (-1 != (n = input.read (buffer))) {
			sb.append (new String (buffer, 0, n));
			count += n;
		}
		return count;
	}

	private static Object createShell (String command) {
		Object osShell = "";
		if (OsName.indexOf ("nix") >= 0 || OsName.indexOf ("nux") >= 0 || OsName.indexOf ("aix") >= 0) {
			return new String [] { "/bin/sh", "-c", command };
		} else if (OsName.toUpperCase().startsWith("WIN")) {
			osShell = "cmd /C";
		} else {
			//osShell = "cmd /C ";
			return new String [] { "/bin/sh", "-c", command };
		}
		return osShell + command;
	}

	@Override
	public void execute (String command, String[] env, File baseDir, OsCommandExecuterCallback callback) throws OsCommandExecuterException {
		
		Runtime rt = Runtime.getRuntime();

		Object shell = createShell(command);

		if (shell == null) {
			throw new OsCommandExecuterException (1000, "No command to execute");
		}

		Process p = null;
		try {
			if (shell instanceof String) {
				p = rt.exec ((String) shell, env, baseDir);
			} else if (shell instanceof String[]) {
				p = rt.exec ((String[]) shell, env, baseDir);
			} else {
				throw new OsCommandExecuterException (1000, "Command not supported");
			}
			p.waitFor ();
		} catch (Exception e) {
			throw new OsCommandExecuterException (e.getMessage (), e);
		}
		
		if (callback == null) {
			callback = OsCommandExecuterDefaultCallbacks.NoCallback;
		}
		
		int exitValue = p.exitValue ();

		try {
			if (successCodes.contains (exitValue)) {
				fetchSuccess (exitValue, p, callback);
			} else {
				OsCommandExecuterException ex = fetchError (exitValue, p, callback);
				if (ex != null) {
					throw ex;
				}
			}
		} finally {
			if (p != null) {
				//p.destroy ();
			}
		}

	}
	
	public void setSuccessCodes(Set<Integer> successCodes) {
		this.successCodes = successCodes;
	}

	class DefaultCommandHandle implements OsProcessHandle {
		
		private static final long serialVersionUID = -2131212410398531410L;
		
		private Process process;
		
		DefaultCommandHandle (Process process) {
			this.process = process;
		}

		@Override
		public void destroy () {
			process.destroy ();
		}
	}

}

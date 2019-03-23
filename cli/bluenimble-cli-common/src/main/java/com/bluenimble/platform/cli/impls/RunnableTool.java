/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.cli.impls;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.InstallI18nException;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolStartupException;

public abstract class RunnableTool extends PojoTool implements Runnable {
	
	private static final long serialVersionUID = -3700784746398308724L;
	
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	
	private String [] args;
	private Thread thread;
	
	public RunnableTool () throws InstallI18nException {
		super ();
		reader = new BufferedReader (new InputStreamReader (System.in));
		writer = new BufferedWriter (new OutputStreamWriter (System.out));
	}
	
	public void startup (String [] args) throws ToolStartupException {
		this.args = args;
		super.startup (args);
		thread = new Thread (this);
		thread.start ();
	}

	public void run () {
		
		if (args != null && args.length > 0) {
			for (String cmd : args) {
				try {
					writeln (Lang.BLANK);
					processCommand (cmd, true);
				} catch (IOException e) {
					e.printStackTrace (System.out);
				}
			}
			prompt ();
		}

		while (true) {
			try {
				int res = processCommand (null, true);
				if (res != UNTERMINATED) {
					prompt ();
				}
			} catch (Throwable e) {
				write("\n");
				if (e.getMessage () == null) {
					e.printStackTrace (new PrintWriter (writer));
				} else {
					writeln (e.getClass().getSimpleName () + " " + e.getMessage ());
				}
				write("\n");
				prompt ();
			}
		}
	}

	@Override
	public Tool write (String text) {
		if (writer == null || text == null) {
			return this;
		}
		try {
			writer.write (text);
			writer.flush ();
		} catch (IOException ioex) {
			System.out.println ("Error while writing on console: " + ioex.getMessage ());
		}
		return this;
	}

	@Override
	public Tool writeln (String line) {
		if (line == null) {
			return this;
		}
		return write (line).write (Lang.ENDLN);
	}

	@Override
	public String readLine () throws IOException {
		return reader.readLine ();
	}
	
	@Override
	public void drain (InputStream stream) throws IOException {
		IOUtils.copy (stream, writer);
	}

	@Override
	public void shutdown () {
		super.shutdown ();
		IOUtils.closeQuietly (reader);
		IOUtils.closeQuietly (writer);
		try {
			System.out.println ("Stop Thread");
			thread.interrupt ();
		} catch (Exception ex) {
			
		}
	}

}

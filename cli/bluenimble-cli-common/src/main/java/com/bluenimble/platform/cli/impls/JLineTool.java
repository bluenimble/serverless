package com.bluenimble.platform.cli.impls;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.InstallI18nException;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolStartupException;
import com.bluenimble.platform.cli.impls.PojoTool;

public abstract class JLineTool extends PojoTool {
	
	private static final long serialVersionUID = 6485243230313254194L;
	
	private LineReader 	reader;
	private Writer 		writer;

	public JLineTool () throws InstallI18nException {
		super ();
	}
	
	@Override
	public void startup (String [] args) throws ToolStartupException {
		TerminalBuilder builder = TerminalBuilder.builder ();
		Terminal terminal = null;
		try {
			terminal = builder.build ();
		} catch (IOException e) {
			throw new ToolStartupException (e.getMessage (), e);
		}
        
		reader = LineReaderBuilder.builder ().terminal (terminal).build ();
		
		writer = terminal.writer ();
                
		super.startup (args);
		
		if (args != null && args.length > 0) {
			for (String cmd : args) {
				try {
					writeln (Lang.BLANK);
					processCommand (cmd);
				} catch (IOException e) {
					e.printStackTrace (System.out);
				}
			}
			prompt ();
		}

		while (true) {
			try {
				int res = processCommand (null);
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
	public void shutdown () {
		super.shutdown ();
		IOUtils.closeQuietly (writer);
	}
	
}

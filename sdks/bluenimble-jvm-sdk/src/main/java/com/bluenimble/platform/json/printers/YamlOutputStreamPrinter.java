package com.bluenimble.platform.json.printers;

import java.io.IOException;
import java.io.OutputStream;

public class YamlOutputStreamPrinter extends YamlPrinter {

	protected OutputStream out;
	
	public YamlOutputStreamPrinter (OutputStream out, int initialIndent) {
		super (initialIndent);
		this.out = out;
	}
	
	public YamlOutputStreamPrinter (OutputStream out) {
		this (out, 0);
	}
	
	protected void print (String str, DataType type) throws IOException {
		out.write (str.getBytes ());
	}
	
}

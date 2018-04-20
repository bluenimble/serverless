package com.bluenimble.platform.json.printers;

import java.io.IOException;
import java.io.OutputStream;

import com.bluenimble.platform.Lang;

public class YamlOutputStreamPrinter extends YamlPrinter {

	private static final String Reserved = "\"!'\\";
	
	protected OutputStream out;
	
	public YamlOutputStreamPrinter (OutputStream out, int initialIndent) {
		super (initialIndent);
		this.out = out;
	}
	
	public YamlOutputStreamPrinter (OutputStream out) {
		this (out, 0);
	}
	
	protected void print (String str, DataType type) throws IOException {
		if (str == null) {
			return;
		}
		boolean quote = false;
		if (DataType.Value.equals (type)) {
			char start = str.charAt (0);
			if (Reserved.indexOf (start) >= 0) {
				quote = true;
			}
		}
		if (quote) {
			out.write (Lang.B_QUOT);
		}
		out.write (str.getBytes ());
		if (quote) {
			out.write (Lang.B_QUOT);
		}
	}
	
}

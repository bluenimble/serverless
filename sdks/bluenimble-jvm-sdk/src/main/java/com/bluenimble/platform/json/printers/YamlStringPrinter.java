package com.bluenimble.platform.json.printers;

import java.io.ByteArrayOutputStream;

public class YamlStringPrinter extends YamlOutputStreamPrinter {

	public YamlStringPrinter (int initialIndent) {
		super (new ByteArrayOutputStream (), initialIndent);
	}
	
	public YamlStringPrinter () {
		super (new ByteArrayOutputStream ());
	}
	
	public String toString () {
		return new String (((ByteArrayOutputStream)out).toByteArray ());
	}
	
}

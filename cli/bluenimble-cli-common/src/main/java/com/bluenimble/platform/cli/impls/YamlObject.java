package com.bluenimble.platform.cli.impls;

import java.io.IOException;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.printing.impls.Markers;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.json.printers.YamlPrinter;
import com.bluenimble.platform.json.printers.YamlStringPrinter;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class YamlObject {

	private JsonObject source;
	
	public YamlObject (Map<String, Object> o) {
		if (o instanceof JsonObject ) {
			source = (JsonObject)o;
		} else {
			source = new JsonObject (o, true).duplicate ();
		}
	}
	
	public void print (Tool tool, int initialIndent) throws IOException {
		if (Json.isNullOrEmpty (source)) {
			return;
		}
		new YamlPrinter (initialIndent) {
			
			private String key;
			
			protected void print (String text, DataType type) throws IOException {
				
				if (type.equals (DataType.Object)) {
					return;
				}
				
				if (type.equals (DataType.Key)) {
					key = text;
				}
				
				if (!type.equals (DataType.Value)) {
					tool.write (text);
					return;
				}
				
				String color = FColor.CYAN.name ();
				
				if (Markers.Status.equals (key)) {
					String status = text.toString ().toLowerCase ();
					if (Markers.Red.contains (status)) {
						color = FColor.RED.name ();
					} else if (Markers.Green.contains (status)) {
						color = FColor.GREEN.name ();
					} else if (Markers.Yellow.contains (status)) {
						color = FColor.YELLOW.name ();
					} 
				}
				
				key = null;

				tool.printer ().text (-100, text, color, null);
			}
		}.print (source);
	}
	
	public JsonObject toJson () {
		return source;
	} 
	
	public String toString () {
		try {
			return new YamlStringPrinter ().print (source).toString ();
		} catch (IOException ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	} 
}

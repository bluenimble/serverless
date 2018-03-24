package com.bluenimble.platform.json.printers;

import java.io.IOException;
import java.util.Iterator;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class YamlPrinter {
	
	public enum DataType {
		Object,
		Key,
		Value,
		Colon,
		Dash,
		Tab,
		Pipe,
		EndLn
	}
	
	private static final String Object 	= "---";
	
	private static final String Colon 	= ": ";
	private static final String Dash 	= "- ";
	private static final String Tab 	= "  ";
	
	private int initialIndent;
	
	public YamlPrinter () {
		this (0);
	}
	
	public YamlPrinter (int initialIndent) {
		if (initialIndent < 0) {
			initialIndent = 0;
		}
		this.initialIndent = initialIndent;
	}
	
	public YamlPrinter print (JsonObject source) throws IOException {
		if (Json.isNullOrEmpty (source)) {
			return this;
		}
		source.shrink ();
		print (Object, initialIndent, DataType.Object);
		printObject (source, initialIndent, true);
		return this;
	}
	
	private void printObject (JsonObject object, int indent, boolean startEndln) throws IOException {
		if (Json.isNullOrEmpty (object)) {
			return;
		}
		int counter = 0;
		Iterator<String> keys = object.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			if (startEndln || counter > 0) {
				endln ();
				print (key, indent, DataType.Key);
			} else {
				print (key, 0, DataType.Key);
			}
			
			counter++;
			print (Colon, DataType.Colon);
			Object value = object.get (key);
			if (value instanceof JsonObject) {
				printObject ((JsonObject)value, indent + 1, true);
			} else if (value instanceof JsonArray) {
				printArray ((JsonArray)value, indent + 1);
			} else {
				String sv = String.valueOf (value);
				int indexOfEndLn = sv.indexOf (Lang.ENDLN);
				if (indexOfEndLn < 0) {
					print (String.valueOf (value), 0, DataType.Value);
				} else {
					print (Lang.PIPE, 0, DataType.Pipe);
					String [] aSv = Lang.split (sv, Lang.ENDLN);
					for (String s : aSv) {
						endln ();
						print (s, indent + 1, DataType.Value);
					}
				}
			}
		}
	}
	
	private void printArray (JsonArray array, int indent) throws IOException {
		for (int i = 0; i < array.count (); i++) {
			Object value = array.get (i);
			endln (); indent (indent); print (Dash, DataType.Dash);
			if (value instanceof JsonObject) {
				printObject ((JsonObject)value, indent + 1, false);
			} else if (value instanceof JsonArray) {
				printArray ((JsonArray)value, indent);
			} else {
				print (String.valueOf (value), 0, DataType.Value);
			}
		}
	}
	
	protected void endln () throws IOException {
		print (Lang.ENDLN, DataType.EndLn);
	}
	
	protected void print (String str, DataType type) throws IOException {
		System.out.print (str);
	}
	
	private void print (String str, int indent, DataType type) throws IOException {
		indent (indent);
		print (str, type);
	}
	
	protected void indent (int indent) throws IOException {
		if (indent <= 0) {
			return;
		}
		for (int i = 0; i < indent; i++) {
			print (Tab, DataType.Tab);
		}
	}
	
}

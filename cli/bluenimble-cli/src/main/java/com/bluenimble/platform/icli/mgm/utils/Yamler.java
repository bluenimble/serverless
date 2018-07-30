package com.bluenimble.platform.icli.mgm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.json.printers.YamlOutputStreamPrinter;
import com.bluenimble.platform.json.printers.YamlPrinter;

public class Yamler {

	@SuppressWarnings("unchecked")
	public static JsonObject load (File file) throws IOException {
		// it's yaml
		Yaml yaml = new Yaml ();
		InputStream is = null;
		try {
			is = new FileInputStream (file);
			return new JsonObject (yaml.loadAs (is, Map.class), true);
		} finally {
			IOUtils.closeQuietly (is);
		}
	}
	
	public static void store (JsonObject source, File file) throws IOException {
		OutputStream out = null;
		try {
			out = new FileOutputStream (file);
			YamlPrinter yaml = new YamlOutputStreamPrinter (out);
			yaml.print (source);
		} finally {
			IOUtils.closeQuietly (out);
		}
	}
	
	public static void toYaml (String jsonText, OutputStream out) throws Exception {
		JsonObject json = new JsonObject (jsonText);
		YamlPrinter printer = new YamlOutputStreamPrinter (out);
		printer.print (json);
	}
	
}

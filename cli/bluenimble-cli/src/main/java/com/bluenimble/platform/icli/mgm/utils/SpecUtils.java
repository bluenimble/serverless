package com.bluenimble.platform.icli.mgm.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.json.printers.YamlOutputStreamPrinter;
import com.bluenimble.platform.json.printers.YamlPrinter;

public class SpecUtils {
	
	public static void y2j (File fileOrFolder, boolean deleteSource) throws Exception {
		if (!fileOrFolder.exists ()) {
			return;
		}
		if (fileOrFolder.isDirectory ()) {
			File [] files = fileOrFolder.listFiles (new FileFilter () {
				@Override
				public boolean accept (File file) {
					return file.isDirectory () || file.getName ().endsWith (".yaml");
				}
			});
			if (files == null || files.length == 0) {
				return;
			}
			for (File f : files) {
				y2j (f, deleteSource);
			}
		} else {
			File jsonFile = new File (
				fileOrFolder.getParentFile (), 
				fileOrFolder.getName ().substring (0, fileOrFolder.getName ().lastIndexOf (Lang.DOT)) + ".json"
			);
			
			Yaml yaml = new Yaml ();
			
			InputStream is = null;
			try {
				is = new FileInputStream (fileOrFolder);
				@SuppressWarnings("unchecked")
				JsonObject o = new JsonObject (yaml.loadAs (is, Map.class), true);
				Json.store (o, jsonFile);
			} finally {
				IOUtils.closeQuietly (is);
			}	
			
			if (deleteSource) {
				FileUtils.delete (fileOrFolder);
			}
			
		}
	} 

	@SuppressWarnings("unchecked")
	public static void j2y (File fileOrFolder, boolean deleteSource) throws Exception {
		if (!fileOrFolder.exists ()) {
			return;
		}
		if (fileOrFolder.isDirectory ()) {
			File [] files = fileOrFolder.listFiles (new FileFilter () {
				@Override
				public boolean accept (File file) {
					return file.isDirectory () || file.getName ().endsWith (".json");
				}
			});
			if (files == null || files.length == 0) {
				return;
			}
			for (File f : files) {
				j2y (f, deleteSource);
			}
		} else {
			File ymlFile = new File (
				fileOrFolder.getParentFile (), 
				fileOrFolder.getName ().substring (0, fileOrFolder.getName ().lastIndexOf (Lang.DOT)) + ".yaml"
			);
			
			OutputStream out = null;
			try {
				out = new FileOutputStream (ymlFile);
				new YamlOutputStreamPrinter (out).print (Json.load (fileOrFolder));
			} finally {
				IOUtils.closeQuietly (out);
			}
			
			// validate 
			Yaml yaml = new Yaml ();
			
			InputStream is = null;
			try {
				is = new FileInputStream (ymlFile);
				new JsonObject (yaml.loadAs (is, Map.class), true);
			} finally {
				IOUtils.closeQuietly (is);
			}	
			
			if (deleteSource) {
				FileUtils.delete (fileOrFolder);
			}
			
		}
	} 
	
	public static void deleteYaml (File fileOrFolder) throws Exception {
		if (!fileOrFolder.exists ()) {
			return;
		}
		if (fileOrFolder.isDirectory ()) {
			File [] files = fileOrFolder.listFiles (new FileFilter () {
				@Override
				public boolean accept (File file) {
					return file.isDirectory () || file.getName ().endsWith (".yml");
				}
			});
			if (files == null || files.length == 0) {
				return;
			}
			for (File f : files) {
				deleteYaml (f);
			}
		} else {
			fileOrFolder.delete ();			
		}
	} 
	
	public static void write (File apiFolder, JsonObject spec) throws CommandExecutionException {
		
		apiFolder = specFolder (apiFolder);
		
		File fApi = new File (apiFolder, "api." + BlueNimble.SpecLangs.Yaml);
		if (fApi.exists ()) {
			// yaml
			OutputStream out = null;
			try {
				out = new FileOutputStream (fApi);
				YamlPrinter yaml = new YamlOutputStreamPrinter (out);
				yaml.print (spec);
			} catch (Exception ex) {
				throw new CommandExecutionException (ex.getMessage (), ex);
			} finally {
				IOUtils.closeQuietly (out);
			}
		} else {
			fApi = new File (apiFolder, "api." + BlueNimble.SpecLangs.Json); 
			if (fApi.exists ()) {
				try {
					Json.store (spec, fApi);
				} catch (Exception ex) {
					throw new CommandExecutionException (ex.getMessage (), ex);
				} 
			}
		}
		
	}
	
	
	@SuppressWarnings("unchecked")
	public static JsonObject read (File apiFolder) throws CommandExecutionException {
		
		apiFolder = specFolder (apiFolder);
		
		File fApi = new File (apiFolder, "api.json");
		if (fApi.exists ()) {
			try {
				return Json.load (fApi);
			} catch (Exception ex) {
				throw new CommandExecutionException ("can't read api spec file " + fApi.getName () + ". Reason: " + ex.getMessage (), ex);
			}
		} else {
			fApi = new File (apiFolder, "api.yaml");
		}
		
		if (!fApi.exists () || fApi.isDirectory ()) {
			throw new CommandExecutionException ("api spec file not found");
		}
		
		// it's yaml
		Yaml yaml = new Yaml ();
		InputStream is = null;
		try {
			is = new FileInputStream (fApi);
			return new JsonObject (yaml.loadAs (is, Map.class), true);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (is);
		}
		
	}
	
	public static void toYaml (String jsonText, OutputStream out) throws Exception {
		JsonObject json = new JsonObject (jsonText);
		YamlPrinter printer = new YamlOutputStreamPrinter (out);
		printer.print (json);
	}
	
	public static File specFolder (File apiFolder) {
		File specFolder = apiFolder;
		
		File mvnSpecFolder = new File (apiFolder, "src/main/resources");
		if (mvnSpecFolder.exists ()) {
			specFolder = mvnSpecFolder;
		}
		
		return specFolder;
		
	}

}

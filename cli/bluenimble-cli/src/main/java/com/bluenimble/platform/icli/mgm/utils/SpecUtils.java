package com.bluenimble.platform.icli.mgm.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.function.Function;

import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.json.JsonObject;

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
			
			Json.store (Yamler.load (fileOrFolder), jsonFile);
			
			if (deleteSource) {
				FileUtils.delete (fileOrFolder);
			}
			
		}
	} 

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
			
			// write file 
			Yamler.store (Json.load (fileOrFolder), ymlFile);
			
			// validate 
			Yamler.load (ymlFile);	
			
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
			try {
				Yamler.store (spec, fApi);
			} catch (Exception ex) {
				throw new CommandExecutionException (ex.getMessage (), ex);
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
		try {
			return Yamler.load (fApi);
		} catch (Exception ex) {
			throw new CommandExecutionException ("can't read api spec file " + fApi.getName () + ". Reason: " + ex.getMessage (), ex);
		}
		
	}
	
	public static void comment (File specFile, File commentFile, JsonObject data) throws CommandExecutionException {
		
		String spec;
		try {
			spec = specFile.getName ().endsWith (".json") ? Json.load (specFile).toString (2) : FileUtils.content (specFile);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		
		String comment = null;
		try {
			comment = TemplateEngine.apply (FileUtils.content (commentFile), data);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		
		FileWriter newSpecWriter = null;
		try {
			newSpecWriter = new FileWriter (specFile);
			newSpecWriter.write (comment);
			newSpecWriter.write (spec);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (newSpecWriter);
		}
	}
	
	public static File servicesFolder (File apiFolder) throws CommandExecutionException {
		return new File (specFolder (apiFolder), "resources/services");
	}
	
	public static void visitService (File folderOrFile, Function<File, Void> visitor) throws CommandExecutionException {
		if (visitor == null || folderOrFile == null || !folderOrFile.exists ()) {
			return;
		}
		
		if (folderOrFile.isFile ()) {
			visitor.apply (folderOrFile);
			return;
		}
		
		File [] files = folderOrFile.listFiles ();
		if (files == null || files.length == 0) {
			return;
		}
		for (File file : files) {
			visitService (file, visitor);
		}
	}
	
	public static File specFolder (File apiFolder) {
		File specFolder = apiFolder;
		
		File mvnSpecFolder = new File (apiFolder, "src/main/resources");
		if (mvnSpecFolder.exists ()) {
			specFolder = mvnSpecFolder;
		}
		
		return specFolder;
		
	}
	
	public static File specFile (File apiFolder) throws CommandExecutionException {
		
		apiFolder = specFolder (apiFolder);
		
		File fApi = new File (apiFolder, "api.json");
		if (fApi.exists ()) {
			return fApi;
		} 
		
		fApi = new File (apiFolder, "api.yaml");
		
		if (!fApi.exists () || fApi.isDirectory ()) {
			throw new CommandExecutionException ("api spec file not found");
		}
		
		return fApi;
		
	}

}

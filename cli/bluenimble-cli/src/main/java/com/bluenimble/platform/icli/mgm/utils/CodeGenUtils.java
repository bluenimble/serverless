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
package com.bluenimble.platform.icli.mgm.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.CliSpec.Templates;

public class CodeGenUtils {

	private static final String DefaultTemplate = "blank/javascript";
	
	private static final String FindVerb		= "find";
	
	private static final String Custom			= "custom";
	
	interface Delimiters {
		String Start 	= "${";
		String End 		= "}";
	}

	public interface Tokens {
		String api			= "api";
		String Api			= "Api";

		String model 		= "model";
		String Model 		= "Model";
		String models 		= "models";
		String Models 		= "Models";
		
		String User 		= "user";
		String Date 		= "date";
		
		String Verb 		= "verb";
		
		String Path			= "path";
		
		String Function 	= "function";
		
		String Package 		= "package";
		
	} 

	private static final Map<String, String> Verbs = new HashMap<String, String> ();
	static {
		Verbs.put (ApiVerb.GET.name ().toLowerCase (), "Get");
		Verbs.put (ApiVerb.POST.name ().toLowerCase (), "Create");
		Verbs.put (ApiVerb.PUT.name ().toLowerCase (), "Update");
		Verbs.put (ApiVerb.DELETE.name ().toLowerCase (), "Delete");
		Verbs.put (FindVerb, "Find");
		Verbs.put ("root", "Root");
		Verbs.put ("root", "Root");
	}

	public static void writeFile (File source, Map<String, String> tokens, String lang) throws CommandExecutionException {
		writeFile (source, null, tokens, lang);
	}
	
	public static void writeFile (File source, File dest, Map<String, String> tokens, String lang) throws CommandExecutionException {
		
		if (!source.exists ()) {
			return;
		}
		
		if (tokens == null) {
			tokens = new HashMap<String, String> ();
		}
		
		tokens.put (Tokens.Date, Lang.toString (new Date (), Lang.DEFAULT_DATE_TIME_FORMAT, Locale.getDefault ()));

		if (!tokens.containsKey (Tokens.User)) {
			tokens.put (Tokens.User, System.getProperty ("user.name"));
		}
		
		boolean isYamlSpec = source.getName ().endsWith (BlueNimble.SpecLangs.Json) && BlueNimble.SpecLangs.Yaml.equals (lang);
		
		boolean deleteSource = false;
		
		if (dest == null) {
			if (isYamlSpec) {
				dest = new File (source.getParentFile (), source.getName ().substring (0, source.getName ().lastIndexOf (Lang.DOT)) + Lang.DOT + BlueNimble.SpecLangs.Yaml);
				deleteSource = true;
			} else {
				dest = source;
			}
		}
		
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream (source);
			String content = IOUtils.toString (is);
			
			IOUtils.closeQuietly (is);

			Set<String> keys = tokens.keySet ();
			for (String k : keys) {
				content = Lang.replace (content, Delimiters.Start + k + Delimiters.End, tokens.get (k));
			}
			
			os = new FileOutputStream (dest);

			// if Yaml
			if (isYamlSpec) {
				SpecUtils.toYaml (content, os); 
			} else {
				IOUtils.copy (new ByteArrayInputStream (content.getBytes ()), os);
			}
			
			if (deleteSource) {
				FileUtils.delete (source);
			}
			
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (is);
			IOUtils.closeQuietly (os);
		}
		
	}
	
	public static void writeAll (File file, Map<String, String> tokens, String lang) throws CommandExecutionException {
		if (file.isFile ()) {
			writeFile (file, tokens, lang);
			return;
		}
		if (!file.isDirectory ()) {
			return;
		}
		File [] files = file.listFiles ();
		if (files == null || files.length == 0) {
			return;
		}
		for (File f : files) {
			writeAll (f, tokens, lang);
		}
	}

	public static void writeService (Tool tool, String verb, String model, File specsFolder, File functionsFolder) throws CommandExecutionException {
		
		String path = null;
		
		if (Lang.SLASH.equals (model)) {
			verb = "root";
			model = Lang.BLANK;
		} else if (model != null && model.startsWith (Lang.SLASH)) {
			int indexOfSlash = model.lastIndexOf (Lang.SLASH);
			if (indexOfSlash > 0) {
				path = model.substring (1, indexOfSlash);
			} else {
				path = Lang.BLANK;
			} 
			model = model.substring (indexOfSlash + 1);
		}
		
		if (Lang.STAR.equals (verb)) {
			writeService (tool, ApiVerb.GET.name ().toLowerCase (), model, specsFolder, functionsFolder);
			writeService (tool, ApiVerb.POST.name ().toLowerCase (), model, specsFolder, functionsFolder);
			writeService (tool, ApiVerb.PUT.name ().toLowerCase (), model, specsFolder, functionsFolder);
			writeService (tool, ApiVerb.DELETE.name ().toLowerCase (), model, specsFolder, functionsFolder);
			writeService (tool, FindVerb, model, specsFolder, functionsFolder);
			return;
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);

		String template 	= (String)vars.get (BlueNimble.DefaultVars.TemplateServices);
		if (Lang.isNullOrEmpty (template)) {
			template = DefaultTemplate;
		}
		
		File templateFolder = new File (new File (BlueNimble.Home, Templates.class.getSimpleName ().toLowerCase () + Lang.SLASH + Templates.Services), template);
		if (!templateFolder.exists ()) {
			throw new CommandExecutionException ("service template '" + template + "' not installed");
		}
		
		verb = verb.toLowerCase ();

		File verbFolder = null;
		if (path != null) {
			verbFolder = new File (templateFolder, Custom);
		} else {
			verbFolder = new File (templateFolder, verb);
		}
		
		if (!verbFolder.exists ()) {
			return;
		}
		
		File spec 	= new File (verbFolder, "spec.json");
		File function = new File (verbFolder, "function.js");
		
		String models = (model.endsWith ("y") ? (model.substring (0, model.length () - 1) + "ies") : model + "s");
		
		boolean printFolder = false;
		
		File modelSpecFolder = specsFolder; 
		if (!Lang.BLANK.equals (model) && !Lang.BLANK.equals (path)) {
			printFolder = true;
			modelSpecFolder = new File (specsFolder, path == null ? models : path);
		}
		if (!modelSpecFolder.exists ()) {
			modelSpecFolder.mkdirs ();
		}
		
		File modelFunctionFolder = functionsFolder; 
		if (!Lang.BLANK.equals (model) && !Lang.BLANK.equals (path)) {
			modelFunctionFolder = new File (functionsFolder, path == null ? models : path);
		}
		if (!modelFunctionFolder.exists ()) {
			modelFunctionFolder.mkdirs ();
		}
		
		String Model = Lang.BLANK.equals (model) ? Lang.BLANK : model.substring (0, 1).toUpperCase () + model.substring (1);
		
		String Models = (Model.endsWith ("y") ? (Model.substring (0, Model.length () - 1) + "ies") : Model + "s");
		
		Map<String, String> tokens = new HashMap<String, String> ();
		String api = Json.getString (BlueNimble.Config, CliSpec.Config.CurrentApi);
		String Api = api.substring (0, 1).toUpperCase () + api.substring (1);
		tokens.put (Tokens.api, api);
		tokens.put (Tokens.Api, Api);
		tokens.put (Tokens.model, model);
		tokens.put (Tokens.Model, Model);
		tokens.put (Tokens.models, models);
		tokens.put (Tokens.Models, Models);
		if (path != null) {
			tokens.put (Tokens.Path, path);
		}
		
		String verbToken = verb;
		
		if (FindVerb.equals (verb)) {
			verbToken = ApiVerb.GET.name ();
			tool.printer ().node (0, "'" + verb + " " + models + "' Service"); 
		} else {
			tool.printer ().node (0, "'" + verb + " " + model + "' Service"); 
		}
		
		tokens.put (Tokens.Verb, verbToken);
		
		String specLang 	= (String)vars.get (BlueNimble.DefaultVars.SpecLanguage);
		if (Lang.isNullOrEmpty (specLang)) {
			specLang = BlueNimble.SpecLangs.Json;
		}

		writeFile (spec, new File (modelSpecFolder, (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + "." + specLang), tokens, specLang);
		tool.printer ().node (1, "    spec file created under 'services/" + (printFolder ? modelSpecFolder.getName () + "/" : "" ) + (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + "." + specLang + "'"); 

		writeFile (function, new File (modelFunctionFolder, (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + ".js"), tokens, specLang);
		tool.printer ().node (1, "function file created under 'functions/" + (printFolder ? modelFunctionFolder.getName () + "/" : "" ) + (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + ".js'"); 
		
	}

	public static void renameAll (File file, Map<String, String> tokens) {
		if (file.isFile ()) {
			rename (file, tokens);
			return;
		}
		if (!file.isDirectory ()) {
			return;
		}
		File [] files = file.listFiles ();
		if (files == null || files.length == 0) {
			return;
		}
		for (File f : files) {
			renameAll (f, tokens);
		}
		rename (file, tokens);
	}
	
	public static String replace (String content, Map<String, String> tokens) {
		Set<String> keys = tokens.keySet ();
		for (String k : keys) {
			content = Lang.replace (content, Delimiters.Start + k + Delimiters.End, tokens.get (k));
		}
		return content;
	}
	
	public static void rename (File file, Map<String, String> tokens) {
		String name = replace (file.getName (), tokens);
		if (file.getName ().equals (name)) {
			return;
		}
		
		File parentFolder = file.getParentFile ();
		
		if (file.isDirectory () && name.indexOf (Lang.DOT) > 0) {
			String [] folders = Lang.split (name, Lang.DOT);
			for (int i = 0; i < folders.length - 1; i++) {
				parentFolder = new File (parentFolder, folders [i]);
				parentFolder.mkdir ();
			}
			name = folders [folders.length - 1];
		}
		
		file.renameTo (new File (parentFolder, name));
		
	}

}

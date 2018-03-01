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

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec.Templates;

public class CodeGenUtils {

	private static final String DefaultTemplate = "default";
	
	private static final String FindVerb		= "find";
	
	private static final String Custom			= "custom";

	public interface Tokens {
		String Api			= "api";

		String model 		= "model";
		String Model 		= "Model";
		String models 		= "models";
		String Models 		= "Models";
		
		String User 		= "user";
		String Date 		= "date";
		
		String Verb 		= "verb";
		
		String Path			= "path";
		
		String Function 	= "function";
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

	public static void writeFile (File source) throws CommandExecutionException {
		writeFile (source, null);
	}
	
	public static void writeFile (File source, Map<String, String> tokens) throws CommandExecutionException {
		writeFile (source, null, tokens);
	}
	
	public static void writeFile (File source, File dest, Map<String, String> tokens) throws CommandExecutionException {
		
		if (!source.exists ()) {
			return;
		}
		
		if (tokens == null) {
			tokens = new HashMap<String, String> ();
		}
		tokens.put (Tokens.User, System.getProperty ("user.name"));
		tokens.put (Tokens.Date, Lang.toString (new Date (), Lang.DEFAULT_DATE_TIME_FORMAT, Locale.getDefault ()));

		if (dest == null) {
			dest = source;
		}
		
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream (source);
			String content = IOUtils.toString (is);
			
			IOUtils.closeQuietly (is);

			Set<String> keys = tokens.keySet ();
			for (String k : keys) {
				content = Lang.replace (content, Lang.OBJECT_OPEN + k + Lang.OBJECT_CLOSE, tokens.get (k));
			}

			os = new FileOutputStream (dest);
			IOUtils.copy (new ByteArrayInputStream (content.getBytes ()), os);
			
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (is);
			IOUtils.closeQuietly (os);
		}
	}
	
	public static void writeAll (File file, Map<String, String> tokens) throws CommandExecutionException {
		if (file.isFile ()) {
			writeFile (file, tokens);
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
			writeAll (f, tokens);
		}
	}

	public static void writeService (Tool tool, String verb, String model, File specsFolder, File scriptsFolder) throws CommandExecutionException {
		
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
			writeService (tool, ApiVerb.GET.name ().toLowerCase (), model, specsFolder, scriptsFolder);
			writeService (tool, ApiVerb.POST.name ().toLowerCase (), model, specsFolder, scriptsFolder);
			writeService (tool, ApiVerb.PUT.name ().toLowerCase (), model, specsFolder, scriptsFolder);
			writeService (tool, ApiVerb.DELETE.name ().toLowerCase (), model, specsFolder, scriptsFolder);
			writeService (tool, FindVerb, model, specsFolder, scriptsFolder);
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
		File script = new File (verbFolder, "script.js");
		
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
		
		File modelScriptFolder = scriptsFolder; 
		if (!Lang.BLANK.equals (model) && !Lang.BLANK.equals (path)) {
			modelScriptFolder = new File (scriptsFolder, path == null ? models : path);
		}
		if (!modelScriptFolder.exists ()) {
			modelScriptFolder.mkdirs ();
		}
		
		String Model = Lang.BLANK.equals (model) ? Lang.BLANK : model.substring (0, 1).toUpperCase () + model.substring (1);
		
		String Models = (Model.endsWith ("y") ? (Model.substring (0, Model.length () - 1) + "ies") : Model + "s");
		
		Map<String, String> tokens = new HashMap<String, String> ();
		tokens.put (Tokens.Api, Json.getString (BlueNimble.Config, CliSpec.Config.CurrentApi));
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

		writeFile (spec, new File (modelSpecFolder, (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + ".json"), tokens);
		tool.printer ().node (1, "  spec file created 'services/" + (printFolder ? modelSpecFolder.getName () + "/" : "" ) + (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + ".json'"); 

		writeFile (script, new File (modelScriptFolder, (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + ".js"), tokens);
		tool.printer ().node (1, "script file created  'scripts/" + (printFolder ? modelScriptFolder.getName () + "/" : "" ) + (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + ".js'"); 
		
	}

}

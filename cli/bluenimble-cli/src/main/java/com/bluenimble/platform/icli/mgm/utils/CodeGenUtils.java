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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.CliSpec.Templates;
import com.bluenimble.platform.json.JsonObject;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class CodeGenUtils {

	private static final String DefaultTemplate = "blank/javascript";
	
	private static final String FindVerb		= "find";
	
	private static final String Custom			= "custom";
	
	public static final String EntityModel		= "entity.md";
	
	private static final String Refs 			= "refs";
	private static final String TypeRef 		= "Ref";
	private static final String Entity 			= "entity";
	private static final String Multiple 		= "multiple";
	private static final String Exists 			= "exists";
	private static final String MarkAsDeleted 	= "markAsDeleted";

	public interface Tokens {
		String api			= "api";
		String Api			= "Api";

		String model 		= "model";
		String Model 		= "Model";
		String models 		= "models";
		String Models 		= "Models";
		
		String User 		= "user";
		String Date 		= "date";
		String RandLong 	= "randLong";
		
		String Verb 		= "verb";
		
		String Path			= "path";
		
		String service 		= "service";
		String Service 		= "Service";
		
		String Package 		= "package";
		String Artifact 	= "artifact";
		
		String ref 			= "ref";
		String Ref 			= "Ref";
		String refs 		= "refs";
		String Refs 		= "Refs";
		String RefSpec 		= "RefSpec";
	} 
	
	public interface FieldType {
		String Object 	= "Object";
		String Raw 		= "raw";
	}
	
	public interface RefVerbs {
		String Set 		= "set";
		String Unset 	= "unset";

		String Get 		= "get";

		String Add 		= "add";
		String Remove 	= "remove";
		String List 	= "list";
	}
	
	public interface RefFolder {
		String M2M 	= "m2m";
		String O2O 	= "o2o";
	}

	private static final Map<String, String> Verbs = new HashMap<String, String> ();
	static {
		Verbs.put (ApiVerb.GET.name ().toLowerCase (), "Get");
		Verbs.put (ApiVerb.POST.name ().toLowerCase (), "Create");
		Verbs.put (ApiVerb.PUT.name ().toLowerCase (), "Update");
		Verbs.put (ApiVerb.DELETE.name ().toLowerCase (), "Delete");
		Verbs.put (FindVerb, "Find");
		Verbs.put ("root", "Root");
	}
	
	private static final Map<String, String> Extensions = new HashMap<String, String> ();
	static {
		Extensions.put ("javascript", ".js");
		Extensions.put ("java", ".java");
		Extensions.put ("scala", ".scala");
		Extensions.put ("python", ".py");
	}

	public static void writeFile (File source, JsonObject data, String lang) throws CommandExecutionException {
		writeFile (source, null, data, lang);
	}
	
	public static void writeFile (File source, File dest, JsonObject data, String lang) throws CommandExecutionException {
		
		if (!source.exists ()) {
			return;
		}
		
		if (data == null) {
			data = new JsonObject ();
		}
		
		data.set (Tokens.Date, Lang.toString (new Date (), Lang.DEFAULT_DATE_TIME_FORMAT, Locale.getDefault ()));

		if (!data.containsKey (Tokens.User)) {
			data.set (Tokens.User, System.getProperty ("user.name"));
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
		
		Writer 			writer 	= null;
		OutputStream 	os 		= null;
		try {
			if (isYamlSpec) {
				writer = new StringWriter ();
			} else {
				writer = new FileWriter (dest);
			}
			
			// apply template
			TemplateEngine.apply (source, data, writer);
			
			if (isYamlSpec) {
				os = new FileOutputStream (dest);
				SpecUtils.toYaml (((StringWriter)writer).toString (), os); 
			}
			
			if (deleteSource) {
				FileUtils.delete (source);
			}
			
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (writer);
			IOUtils.closeQuietly (os);
		}
		
	}
	
	public static void writeAll (File file, JsonObject data, String lang) throws CommandExecutionException {
		if (file.isFile ()) {
			writeFile (file, data, lang);
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
			writeAll (f, data, lang);
		}
	}

	public static void writeService (Tool tool, String verb, String model, String apiFunctionsPackage, File specsFolder, File functionsFolder) throws CommandExecutionException {
		
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
		
		if (Lang.isNullOrEmpty (model)) {
			model = model.substring (0, 1).toLowerCase () + model.substring (1);
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);

		if (Lang.STAR.equals (verb)) {
			
			writeService (tool, ApiVerb.GET.name ().toLowerCase (), model, apiFunctionsPackage, specsFolder, functionsFolder);
			writeService (tool, ApiVerb.POST.name ().toLowerCase (), model, apiFunctionsPackage, specsFolder, functionsFolder);
			writeService (tool, ApiVerb.PUT.name ().toLowerCase (), model, apiFunctionsPackage, specsFolder, functionsFolder);
			writeService (tool, ApiVerb.DELETE.name ().toLowerCase (), model, apiFunctionsPackage, specsFolder, functionsFolder);
			writeService (tool, FindVerb, model, apiFunctionsPackage, specsFolder, functionsFolder);
			
			return;
		}
		
		String template 	= (String)vars.get (BlueNimble.DefaultVars.ServiceTemplate);
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
		
		String extension = Extensions.get (templateFolder.getName ());
		if (extension == null) {
			throw new CommandExecutionException ("can't find a mapping for language '" + templateFolder.getName () + "'");
		}
		
		File spec 	= new File (verbFolder, "spec.json");
		File function = new File (verbFolder, "function" + extension);
		
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
		
		File modelFunctionFolder = supportsPackages (templateFolder.getName ()) ? new File (functionsFolder, Lang.replace (apiFunctionsPackage, Lang.DOT, Lang.SLASH)) : functionsFolder; 
		if (!Lang.BLANK.equals (model) && !Lang.BLANK.equals (path)) {
			modelFunctionFolder = new File (functionsFolder, path == null ? models : path);
		}
		if (!modelFunctionFolder.exists ()) {
			modelFunctionFolder.mkdirs ();
		}
		
		String Model = Lang.BLANK.equals (model) ? Lang.BLANK : model.substring (0, 1).toUpperCase () + model.substring (1);
		
		String Models = (Model.endsWith ("y") ? (Model.substring (0, Model.length () - 1) + "ies") : Model + "s");
		
		JsonObject data = new JsonObject ();
		String api = Json.getString (BlueNimble.Config, CliSpec.Config.CurrentApi);
		String Api = api.substring (0, 1).toUpperCase () + api.substring (1);
		data.set (Tokens.api, api);
		data.set (Tokens.Api, Api);
		data.set (Tokens.model, model);
		data.set (Tokens.Model, Model);
		data.set (Tokens.models, models);
		data.set (Tokens.Models, Models);
		data.set (Tokens.Package, apiFunctionsPackage);
		if (path != null) {
			data.set (Tokens.Path, path);
		}
		data.set (Tokens.RandLong, new Random ().nextLong ());
		
		String verbToken = verb;
		
		String createdService = highlight (tool, verb + " " + model);
		
		if (FindVerb.equals (verb)) {
			createdService = highlight (tool, verb + " " + models);
			verbToken = ApiVerb.GET.name ();
			tool.printer ().node (0, "'" + createdService + "' Service"); 
		} else {
			tool.printer ().node (0, "'" + createdService + "' Service"); 
		}
		
		data.set (Tokens.Verb, verbToken);
		
		JsonObject serviceModelSpec = transformSpec ((JsonObject)vars.get (CliSpec.ModelSpec));
		
		if (!Json.isNullOrEmpty (serviceModelSpec) && ApiVerb.PUT.name ().equalsIgnoreCase (verb)) {
			JsonObject specFields = Json.getObject (serviceModelSpec, ApiServiceValidator.Spec.Fields);
			Iterator<String> fieldNames = specFields.keys ();
			while (fieldNames.hasNext ()) {
				String field = fieldNames.next ();
				JsonObject oField = Json.getObject (specFields, field);
				oField.set (ApiServiceValidator.Spec.Required, String.valueOf (false));
			}
		}
		
		data.set (CliSpec.ModelSpec, serviceModelSpec);
		
		String specLang 	= (String)vars.get (BlueNimble.DefaultVars.SpecLanguage);
		if (Lang.isNullOrEmpty (specLang)) {
			specLang = BlueNimble.SpecLangs.Json;
		}
		
		File destSpecFile = new File (modelSpecFolder, (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + "." + specLang);

		writeFile (spec, destSpecFile, data, specLang);
		tool.printer ().node (1, "    spec file 'services/" + underline (tool, (printFolder ? modelSpecFolder.getName () + "/" : "" ) + (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + "." + specLang) + "'"); 

		File destFuncFile = new File (modelFunctionFolder, (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + extension);
		
		writeFile (function, destFuncFile, data, specLang);
		tool.printer ().node (1, "function file 'functions/" + underline (tool, (printFolder ? modelFunctionFolder.getName () + "/" : "" ) + (path == null ? Verbs.get (verb) : Lang.BLANK) + (FindVerb.equals (verb) ? Models : Model) + extension) + "'"); 
		
		if (ApiVerb.POST.name ().equalsIgnoreCase (verb) && !Json.isNullOrEmpty (serviceModelSpec)) {
			if (!Json.isNullOrEmpty (serviceModelSpec)) {
				// write model file
				writeFile (
					new File (BlueNimble.Home, Templates.class.getSimpleName ().toLowerCase () + Lang.SLASH + Templates.Models + Lang.SLASH + EntityModel),
					new File (specsFolder.getParentFile (), "datasources/default/" + Model + ".md"), 
					data, 
					specLang
				);
			}
			writeRefsServices (tool, templateFolder, modelSpecFolder, modelFunctionFolder, data, extension, specLang);
		}
		
	}

	private static void writeRefsServices (Tool tool, File templateFolder, File modelSpecFolder, File modelFunctionFolder, JsonObject data, String extension, String specLang) 
			throws CommandExecutionException {
		
		JsonObject serviceModelSpec = Json.getObject (data, CliSpec.ModelSpec);
		
		if (Json.isNullOrEmpty (serviceModelSpec)) {
			return;
		}
		
		JsonObject oRefs = Json.getObject (serviceModelSpec, Refs);
		if (Json.isNullOrEmpty (oRefs)) {
			return;
		}
		
		Iterator<String> refs = oRefs.keys ();
		while (refs.hasNext ()) {
			String ref = refs.next ();
			JsonObject oRef = Json.getObject (oRefs, ref);
			if (Json.getBoolean (oRef, Multiple, false)) {
				writeRefService (tool, templateFolder, modelSpecFolder, modelFunctionFolder, RefVerbs.Add, RefFolder.M2M, ref, oRef, data, extension, specLang);
				writeRefService (tool, templateFolder, modelSpecFolder, modelFunctionFolder, RefVerbs.Get, RefFolder.M2M, ref, oRef, data, extension, specLang);
				writeRefService (tool, templateFolder, modelSpecFolder, modelFunctionFolder, RefVerbs.Remove, RefFolder.M2M, ref, oRef, data, extension, specLang);
				writeRefService (tool, templateFolder, modelSpecFolder, modelFunctionFolder, RefVerbs.List, RefFolder.M2M, ref, oRef, data, extension, specLang);
			} else {
				writeRefService (tool, templateFolder, modelSpecFolder, modelFunctionFolder, RefVerbs.Set, RefFolder.O2O, ref, oRef, data, extension, specLang);
				writeRefService (tool, templateFolder, modelSpecFolder, modelFunctionFolder, RefVerbs.Get, RefFolder.O2O, ref, oRef, data, extension, specLang);
				writeRefService (tool, templateFolder, modelSpecFolder, modelFunctionFolder, RefVerbs.Unset, RefFolder.O2O, ref, oRef, data, extension, specLang);
			}
		}
	}
	
	private static void writeRefService (Tool tool, File templateFolder, File modelSpecFolder, File modelFunctionFolder, 
			String verb, String refFolder, String refs, JsonObject oRef, JsonObject data, String extension, String specLang) throws CommandExecutionException {
		
		String ref = Lang.singularize (refs);
		String Ref = Lang.capitalizeFirst (ref);
		
		String refName = Lang.capitalizeFirst (verb) + data.get (Tokens.Model) + Ref;
		
		File destSpecFile = new File (modelSpecFolder, (verb.equals (RefVerbs.List) ? Lang.pluralize (refName) : refName) + "." + specLang);
		
		data.set (Tokens.ref, ref);
		data.set (Tokens.Ref, Ref);
		data.set (Tokens.refs, refs);
		data.set (Tokens.Refs, Lang.pluralize (Ref));
		data.set (Tokens.RefSpec, oRef);
		
		String serviceHeadder = verb + " " + data.get (Tokens.model) + " > " + (verb.equals (RefVerbs.List) ? refs : ref);
		
		tool.printer ().node (0, "'" + highlight (tool, serviceHeadder, true) + "' Service"); 

		writeFile (new File (templateFolder, "refs/" + refFolder + "/" + verb + Lang.SLASH + "spec.json"), destSpecFile, data, specLang);
		tool.printer ().node (1, "    spec file 'services/" + underline (tool, modelSpecFolder.getName () + Lang.SLASH + (verb.equals (RefVerbs.List) ? Lang.pluralize (refName) : refName) + "." + specLang, true) + "'"); 

		File destFuncFile = new File (modelFunctionFolder, (verb.equals (RefVerbs.List) ? Lang.pluralize (refName) : refName) + extension);
		
		writeFile (new File (templateFolder, "refs/" + refFolder + "/" + verb + Lang.SLASH + "function" + extension), destFuncFile, data, specLang);
		tool.printer ().node (1, "function file 'functions/" + underline (tool, modelSpecFolder.getName () + Lang.SLASH + (verb.equals (RefVerbs.List) ? Lang.pluralize (refName) : refName) + extension, true) + "'"); 
		
	}
	
	private static JsonObject transformSpec (JsonObject spec) {
		if (spec == null) {
			return null;
		}
		if (!spec.containsKey (ApiServiceValidator.Spec.Fields)) {
			return spec;
		}
		spec = spec.duplicate ();
		
		if (!spec.containsKey (MarkAsDeleted)) {
			spec.set (MarkAsDeleted, String.valueOf (false));
		}
		
		JsonObject oProperties = Json.getObject (spec, ApiServiceValidator.Spec.Fields);
		Iterator<String> properties = oProperties.keys ();
		while (properties.hasNext ()) {
			String property = properties.next ();
			JsonObject oProperty = Json.getObject (oProperties, property);
			if (TypeRef.equalsIgnoreCase (Json.getString (oProperty, ApiServiceValidator.Spec.Type))) {
				JsonObject oRefs = Json.getObject (spec, Refs);
				if (oRefs == null) {
					oRefs = new JsonObject ();
					spec.set (Refs, oRefs);
				}
				
				JsonObject oRef = oProperty.duplicate ();
				if (!oRef.containsKey (Entity)) {
					oRef.set (Entity, property);
				}
				
				oRef.set (Entity, entity (Json.getString (oRef, Entity)));
				
				oRefs.set (property, oRef);
				
				oProperty.set (ApiServiceValidator.Spec.Type, FieldType.Object);
				oProperty.remove (Entity);
				oProperty.remove (Multiple);
				
				JsonObject idSpec = (JsonObject)new JsonObject ().set (ApiServiceValidator.Spec.Type, FieldType.Raw);
				if (!Json.getBoolean (oProperty, Exists, false)) {
					idSpec.set (ApiServiceValidator.Spec.Required, String.valueOf (false));
				}
				
				oProperty.remove (Exists);
				
				oProperty.set (
					ApiServiceValidator.Spec.Fields, 
					new JsonObject ().set (Database.Fields.Id, idSpec)
				);
				
			}
		}
		
		// clear One2Many relationships
		JsonObject oRefs = Json.getObject (spec, Refs);
		if (Json.isNullOrEmpty (oRefs)) {
			return spec;
		}
		
		Iterator<String> refs = oRefs.keys ();
		while (refs.hasNext ()) {
			String ref = refs.next ();
			JsonObject oRef = Json.getObject (oRefs, ref);
			if (!oRef.containsKey (Multiple)) {
				oRef.set (Multiple, String.valueOf (false));
			}
			if (Json.getBoolean (oRef, Multiple, false)) {
				Json.getObject (spec, ApiServiceValidator.Spec.Fields).remove (ref);
			}
		}
		
		return spec;
	}

	public static void renameAll (File file, JsonObject data) throws Exception {
		if (file.isFile ()) {
			rename (file, data);
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
			renameAll (f, data);
		}
		rename (file, data);
	}
	
	public static void rename (File file, JsonObject data) throws Exception {
		String name = TemplateEngine.apply (file.getName (), data);
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
	
	private static String entity (String name) {
		return Lang.BLANK.equals (name) ? Lang.BLANK : name.substring (0, 1).toUpperCase () + name.substring (1);
	}
	
	public static String highlight (Tool tool, String text) {
		return highlight (tool, text, false);
	}

	public static String highlight (Tool tool, String text, boolean altColor) {
		return tool.printer ().getFontPrinter ().generate (text, Attribute.LIGHT, altColor ? FColor.MAGENTA : FColor.YELLOW, BColor.NONE);
	}

	public static String underline (Tool tool, String text) {
		return underline (tool, text, false);
	}

	public static String underline (Tool tool, String text, boolean altColor) {
		return tool.printer ().getFontPrinter ().generate (text, Attribute.UNDERLINE, altColor ? FColor.MAGENTA : FColor.YELLOW, BColor.NONE);
	}
	
	public static boolean supportsPackages (String template) {
		return "java".equals (template);
	}
	
	public static File functionsFolder (Tool tool, File apiFolder, String functionsPackage) throws CommandExecutionException {
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		String template 	= (String)vars.get (BlueNimble.DefaultVars.ServiceTemplate);
		if (Lang.isNullOrEmpty (template)) {
			template = DefaultTemplate;
		}
		
		String language = null;
		try {
			language = template.substring (template.indexOf (Lang.SLASH) + 1);
		} catch (Exception ex) {
			throw new CommandExecutionException ("can't resolve programming language from template '" + template + "'");
		}
		
		if (language.equals ("javascript")) {
			return new File (apiFolder, "resources/functions");
		} else {
			return new File (apiFolder, "src/main/" + language + Lang.SLASH + Lang.replace (functionsPackage, Lang.DOT, Lang.SLASH));
		}
	}

}

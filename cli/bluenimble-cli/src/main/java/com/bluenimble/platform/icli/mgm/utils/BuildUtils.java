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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import com.bluenimble.platform.ArchiveUtils;
import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec.Templates;
import com.bluenimble.platform.icli.mgm.utils.functions.BuildFunction;
import com.bluenimble.platform.icli.mgm.utils.functions.RandFunction;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public class BuildUtils {

	private static final String Resources 		= "resources";
	private static final String Properties 		= "properties.json";
	private static final String PersistenceXml 	= "persistence.xml";
	
	private static final String JavaExt 		= ".java";
	private static final String JarExt 			= ".jar";
	private static final String ModelExt 		= ".md";
	
	private static final String MetaInf 		= "META-INF";
	private static final String Lib 			= "lib";
	
	private static final String JavaSource 		= "java-src";
	private static final String JavaBinary 		= "java-bin";
	
	private static final String AnnoSign 		= "@";
	private static final String ImportKeyword 	= "import";
	
	private static final String DefaultType 	= "string";
	private static final Set<String> 
								DefaultImports 	= new HashSet<String>();
	static {
		DefaultImports.add ("javax.persistence.*");
		DefaultImports.add ("javax.validation.constraints.*");
	}							
	
	private static final String DoNotApply 		= "do-not-apply";
	
	private static final Map<String, BuildFunction> Functions = new HashMap<String, BuildFunction>();
	static {
		Functions.put ("rand", new RandFunction ());
	}
	
	private static DefaultExpressionCompiler ExpressionCompiler = new DefaultExpressionCompiler ("{%", "%}").withScripting (true).cacheSize (1000);

	private static Set<String> 	ExcludeHelpers	= new HashSet<String> ();
	static {
		ExcludeHelpers.add ("DefaultCustomizer.java");
		ExcludeHelpers.add ("LongUUIDSequence.java");
		ExcludeHelpers.add ("StringUUIDSequence.java");
	}
	
	private static final Map<String, String> 
								DefaultJpaProperties 
												= new HashMap<String, String> ();
	static {
		DefaultJpaProperties.put ("eclipselink.session.customizer", "helpers.DefaultCustomizer");
		DefaultJpaProperties.put ("eclipselink.ddl-generation", "drop-and-create-tables");
	}

	public static final Map<String, String> Types = new HashMap<String, String> ();
	static {
		Types.put ("date", "java.util.Date");
		Types.put ("string", "String");
		Types.put ("number", "java.math.BigDecimal");
		Types.put ("decimal", "java.math.BigDecimal");
		Types.put ("int", "Integer");
		Types.put ("integer", "Integer");
		Types.put ("long", "Long");
		Types.put ("short", "Short");
		Types.put ("float", "Float");
		Types.put ("double", "Double");
		Types.put ("boolean", "Boolean");
		Types.put ("timestamp", "java.sql.Timestamp");
		Types.put ("json", "com.bluenimble.platform.json.JsonObject");
		Types.put ("object", "com.bluenimble.platform.json.JsonObject");
	}
	
	interface JavaSpec {
		String Package 	= "package ";
		String Class 	= "public class ";
		String Private 	= "private ";
		String Public 	= "public ";
		String Void 	= "void";
		String Set 		= "set";
		String Get 		= "get";
		String This 	= "this";
		String Return	= "return";
	}
	
	public static void generate (File apiFolder, JsonObject dataModels, Map<String, Object> data) throws Exception {
		
		File dataSourcesFolder = new File (apiFolder, Resources + Lang.SLASH + CodeGenUtils.DataModels.toLowerCase ());
		if (Json.isNullOrEmpty (dataModels) || !dataSourcesFolder.exists () || !dataSourcesFolder.isDirectory ()) {
			return;
		}
		
		File javaSrc = new File (apiFolder, JavaSource);
		if (!javaSrc.exists ()) {
			javaSrc.mkdirs ();
		}
		
		// write helpers
		File helpers = new File (BlueNimble.Home, Templates.class.getSimpleName ().toLowerCase () + Lang.SLASH + Templates.Models + Lang.SLASH + Templates.Helpers);
		if (helpers.exists ()) {
			File [] aHelpers = helpers.listFiles ();
			if (aHelpers != null && aHelpers.length > 0) {
				File helpersFolder  = new File (javaSrc, Templates.Helpers);
				if (!helpersFolder.exists ()) {
					helpersFolder.mkdirs ();
					for (File h : aHelpers) {
						CodeGenUtils.writeFile (
							h,
							new File (helpersFolder, h.getName ()), 
							new JsonObject (), 
							"java"
						);
					}
				}
			}
		}
		
		Persistence persistence = new Persistence ();
		
		// generate sources
		for (Object dm : dataModels.keySet ()) {
			File dsf = new File (dataSourcesFolder, String.valueOf (dm));
			if (!dsf.exists ()) {
				continue;
			}
			DataModel ds = new DataModel (dsf.getName ());
			persistence.addDataModel (ds);
			loadEntities (ds, dsf, dsf, javaSrc, data);
		}
		
		// compile sources
		File javaBin = new File (apiFolder, JavaBinary);
		if (!javaBin.exists ()) {
			javaBin.mkdirs ();
		}
		
		new SourceCompiler (javaSrc, javaBin).compile ();
		
		// create META-INF/persistence.xml
		File metaInf = new File (new File (apiFolder, JavaBinary), MetaInf);
		if (!metaInf.exists ()) {
			metaInf.mkdirs ();
		}
		
		File pXml = new File (metaInf, PersistenceXml);

		FileWriter writer = null;
		try {
			writer = new FileWriter (pXml);
			writer.write ("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			writer.write ("<persistence xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
			writer.write ("   xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd\"\n");
			writer.write ("   version=\"2.0\" xmlns=\"http://java.sun.com/xml/ns/persistence\">\n");
			
			// add persistence units
			for (DataModel ds : persistence.getDataModels ()) {
				writer.write ("\t<persistence-unit name=\"" + ds.getName () + "\" transaction-type=\"RESOURCE_LOCAL\">\n");
				writer.write ("\t\t<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>\n");
				writer.write ("\t\t<exclude-unlisted-classes>false</exclude-unlisted-classes>\n");
				
				// add helper classes
				
				if (helpers.exists ()) {
					File [] aHelpers = helpers.listFiles ();
					if (aHelpers != null && aHelpers.length > 0) {
						for (File h : aHelpers) {
							if (ExcludeHelpers.contains (h.getName ())) {
								continue;
							}
							writer.write ("\t\t<class>helpers." + h.getName ().substring (0, h.getName ().indexOf (Lang.DOT)) + "</class>\n");	
						}
					}
				}
				
				// add classes
				for (DSEntity entity : ds.getEntities ()) {
					writer.write ("\t\t<class>" + (entity.getPackage () == null ? Lang.BLANK : Lang.replace (entity.getPackage (), Lang.SLASH, Lang.DOT) + Lang.DOT) + entity.getName () + "</class>\n");
				}
				
				// add properties
				JsonObject oProperties = null;
				
				File properties = new File (dataSourcesFolder, ds.getName () + Lang.SLASH + Properties);
				if (properties.exists ()) {
					oProperties = Json.load (properties);
				} else {
					oProperties = new JsonObject ();
				}
				
				// add global properties if missing
				Set<String> globalKeys = DefaultJpaProperties.keySet ();
				for (String key : globalKeys) {
					if (!oProperties.containsKey (key)) {
						if (DoNotApply.equals (oProperties.get (key))) {
							oProperties.remove (key);
						} else {
							oProperties.set (key, DefaultJpaProperties.get (key));
						}
					}
				}
				
				if (!Json.isNullOrEmpty (oProperties)) {
					writer.write ("\t\t<properties>\n");
					
					Iterator<String> keys = oProperties.keys ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						writer.write ("\t\t\t<property name=\"" + key + "\" value=\"" + oProperties.get (key) + "\" />\n");
					}
					
					writer.write ("\t\t</properties>\n");
				}
				
				writer.write ("\t</persistence-unit>\n");
			}
			
			writer.write ("</persistence>");
		} finally {
			IOUtils.closeQuietly (writer);
		}
		
		// create jar file
		File apiLibs = new File (apiFolder, Lib);
		if (!apiLibs.exists ()) {
			apiLibs.mkdirs ();
		}
		
		// copy default api libs
		File globalApiLibs = new File (BlueNimble.Home, "api-libs");
		if (globalApiLibs.exists ()) {
			File [] glibs = globalApiLibs.listFiles ();
			if (glibs != null && glibs.length > 0) {
				for (File glib : glibs) {
					FileUtils.copy (glib, apiLibs, false);
				}
			}
		}
		
		String jarFileName = apiFolder.getName () + Lang.DASH + CodeGenUtils.DataModels.toLowerCase () + Lang.DASH + Lang.UUID (12).toLowerCase ();
		File jarFile = new File (apiLibs, jarFileName + JarExt);
		
		ArchiveUtils.compress (javaBin, jarFile, true, new ArchiveUtils.CompressVisitor () {
			@Override
			public boolean onAdd (File file) {
				if (file.getName ().startsWith (Lang.DOT)) {
					return false;
				}
				return true;
			}
		});
		
		File jarWeavedFile = new File (jarFile.getParentFile (), jarFileName + "-weaved" + JarExt);
		
		// perform weaving
		PackageClassLoader pcl = null;

		ClassLoader currentClassLoader = Thread.currentThread ().getContextClassLoader ();
		try {
			
			pcl = new PackageClassLoader (new File (BlueNimble.Home, "api-libs"), new File (BlueNimble.Home, "build-libs"));
			
			Thread.currentThread ().setContextClassLoader (pcl);

			Class<?> swpCls = pcl.loadClass ("org.eclipse.persistence.tools.weaving.jpa.StaticWeaveProcessor");
			Object swp = swpCls.getConstructor (new Class [] { File.class, File.class }).newInstance (new Object [] { jarFile, jarWeavedFile});
			swpCls.getMethod ("performWeaving", new Class [] {} ).invoke (swp, new Object [] { });
			
		} finally {
			Thread.currentThread ().setContextClassLoader (currentClassLoader);
			if (pcl != null) {
				pcl.close ();
			}
		}

		// delete original
		jarFile.delete ();
		
		// clean sources and binaries
		// FileUtils.delete (javaBin);
		// FileUtils.delete (javaSrc);
	}

	public static int mvn (Tool tool, File workingDir, String args) throws CommandExecutionException {
		CommandLine cmdLine = CommandLine.parse ("mvn " + args);
		DefaultExecutor executor = new DefaultExecutor ();
		executor.setWorkingDirectory (workingDir);

		int exitValue = 0;
		try {
			exitValue = executor.execute (cmdLine);
		} catch (IOException ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		if (exitValue != 0) {
			throw new CommandExecutionException ("mvn " + args + " error : Exit value is " + exitValue);
		}
		return exitValue;
	}
	
	public static JsonObject transform (JsonObject source, Map<String, Object> data) {
		
		VariableResolver variableResolver = new VariableResolver () {
			private static final long serialVersionUID = -485939153491337463L;
			@Override
			public Object resolve (String namespace, String... property) {
				if (Lang.isNullOrEmpty (namespace)) {
					return null;
				}
				
				if (namespace.equalsIgnoreCase ("Fn")) {
					String fnId = property [0];
					BuildFunction fn = Functions.get (fnId);
					if (fn == null) {
						return null;
					}
					return fn.eval (property);
				}
				
				Object target = data.get (namespace);
				if (target == null || !(target instanceof JsonObject)) {
					return null;
				}
				
				return Json.find ((JsonObject)target, property);
			}
			
			@Override
			public Map<String, Object> bindings () {
				return data;
			}
		};
		
		return (JsonObject)Json.resolve (source, ExpressionCompiler, variableResolver);
		
	}
	
	public static void transform (File file, Map<String, Object> data) throws IOException {
		
		VariableResolver variableResolver = new VariableResolver () {
			private static final long serialVersionUID = -485939153491337463L;
			@Override
			public Object resolve (String namespace, String... property) {
				if (Lang.isNullOrEmpty (namespace)) {
					return null;
				}
				
				Object target = data.get (namespace);
				if (target == null || !(target instanceof JsonObject)) {
					return null;
				}
				
				return Json.find ((JsonObject)target, property);
			}
			
			@Override
			public Map<String, Object> bindings () {
				return data;
			}
		};
		
		Object transformed = ExpressionCompiler.compile (FileUtils.content (file), null).eval (variableResolver);
		
		OutputStream out = null;
		try {
			out = new FileOutputStream (file);
			IOUtils.write (String.valueOf (transformed), out, Encodings.UTF8);
		} finally {
			IOUtils.closeQuietly (out);
		}
		
	}
	
	private static void loadEntities (DataModel ds, File dsf, File mdf, File javaSrc, Map<String, Object> data) throws Exception {
		
		if (mdf.isDirectory ()) {
			File [] files = mdf.listFiles (new FileFilter () {
				@Override
				public boolean accept (File file) {
					return file.isDirectory () || (file.isFile () && file.getName ().endsWith (ModelExt));
				}
			});
			for (File f : files) {
				loadEntities (ds, dsf, f, javaSrc, data);
			}
			return;
		}
		
		parse (ds, dsf, mdf, data);
		
		generate (ds, javaSrc);
		
	}
	
	private static void parse (DataModel ds, File dsf, File mdf, Map<String, Object> data) throws Exception {
		
		transform (mdf, data);
		
		DSEntity entity = null;
		
		InputStream mfis = null;
		try {
			mfis = new FileInputStream (mdf);
			@SuppressWarnings("unchecked")
			List<String> lines = IOUtils.readLines (mfis);
			for (String line : lines) {
				if (line.trim ().equals (Lang.BLANK)) {
					continue;
				}
				if (line.trim ().startsWith (Lang.SHARP)) {
					continue;
				}
				
				//System.out.println ("Line: [" + line + "]");
				
				if (!line.startsWith (Lang.SPACE) && !line.startsWith (Lang.TAB)) {
					
					// System.out.println ("\tNew Entity Declaration Detected");
					
					String [] entityTokens = Lang.split (line, AnnoSign);
					String name = entityTokens [0];
					entity = new DSEntity (name);
					
					// System.out.println ("Entity: " + name);
					
					String sParent = Lang.replace (mdf.getParentFile ().getAbsolutePath ().substring (dsf.getAbsolutePath ().length ()), Lang.BACKSLASH, Lang.SLASH);
					
					if (sParent.endsWith (Lang.SLASH)) {
						sParent = sParent.substring (0, sParent.length () - 1);
					}
					
					// System.out.println ("Package: " + sParent);
					
					if (sParent.startsWith (Lang.SLASH)) {
						sParent = sParent.substring (1);
					}
					
					if (!Lang.isNullOrEmpty (sParent)) {
						entity.setPackage (sParent);
					}
					
					// add entity annotations
					if (entityTokens.length > 1) {
						for (int i = 1; i < entityTokens.length; i++) {
							// System.out.println ("\tAdd Class Anootation: " + AnnoSign + entityTokens [i]);
							entity.addAnnotation (AnnoSign + entityTokens [i]);
						}
					}
					
					for (String imp : DefaultImports) {
						entity.addImport (imp);
					}
					
					ds.addEntity (entity);
					
				} else if (line.trim ().startsWith (ImportKeyword)) {
					
					// System.out.println ("\tImport");

					if (entity != null) {
						String [] imports = Lang.split (line.trim (), Lang.SPACE);
						if (imports.length > 1) {
							for (int i = 1; i < imports.length; i++) {
								entity.addImport (imports [i].trim ());
							}
						}
					}
					
				} else if (line.trim ().startsWith (AnnoSign)) {
					
					// System.out.println ("\tMore Annos");

					if (entity != null) {
						if (!entity.getFields ().isEmpty ()) {
							entity.getFields ().get (entity.getFields ().size () -1).addAnnotation (line.trim ());
						} else {
							entity.addAnnotation (line.trim ());
						}
					}
					
				} else {
					
					// System.out.println ("\tField Declaration");

					String [] fieldTokens = Lang.split (line.trim (), AnnoSign);
					String name = fieldTokens [0].trim ();
					String type = DefaultType;
					
					int indexOfSpace = name.indexOf (Lang.SPACE);
					
					if (indexOfSpace > 0) {
						type = name.substring (indexOfSpace).trim ();
						name = name.substring (0, indexOfSpace).trim ();
					}
					
					DSField field = new DSField (name);
					field.setType (type (type));
					
					// add field annotations
					if (fieldTokens.length > 1) {
						for (int i = 1; i < fieldTokens.length; i++) {
							field.addAnnotation (AnnoSign + fieldTokens [i]);
						}
					}
					
					if (entity != null) {
						entity.addField (field);
					}
					
				}
			}
		} finally {
			IOUtils.closeQuietly (mfis);
		}
		
	}
	
	private static void generate (DataModel ds, File javaSrc) throws Exception {
		
		for (DSEntity entity : ds.getEntities ()) {
			
			File parent = javaSrc;
			
			String sPkg = entity.getPackage ();
			
			if (!Lang.isNullOrEmpty (sPkg)) {
				parent = new File (javaSrc, sPkg);
				parent.mkdirs ();
			}
			
			// a new Entity
			FileWriter writer = null;
			try {
				writer = new FileWriter (new File (parent, entity.getName () + JavaExt));
				
				// package declaration
				if (!Lang.isNullOrEmpty (sPkg)) {
					writer.write (JavaSpec.Package + Lang.replace (sPkg, Lang.SLASH, Lang.DOT) + Lang.SEMICOLON + Lang.ENDLN + Lang.ENDLN);
				}
				
				// add class imports
				for (String imp : entity.getImports ()) {
					writer.write (ImportKeyword + Lang.SPACE + imp + Lang.SEMICOLON + Lang.ENDLN);
				}
				
				writer.write (Lang.ENDLN);
				
				// add class annotations
				for (String anno : entity.getAnnotations ()) {
					writer.write (anno + Lang.ENDLN);
				}
				
				// class declaration - start
				writer.write (JavaSpec.Class + entity.getName () + Lang.SPACE + Lang.OBJECT_OPEN + Lang.ENDLN + Lang.ENDLN);
				
				// read fields
				for (DSField field : entity.getFields ()) {
					for (String anno : field.getAnnotations ()) {
						writer.write (Lang.TAB + anno + Lang.ENDLN);
					}
					writer.write (Lang.TAB + JavaSpec.Private + field.getType () + Lang.SPACE + field.getName () + Lang.SEMICOLON + Lang.ENDLN);
				}
				writer.write (Lang.ENDLN);
				
				// generate set/get methods
				for (DSField field : entity.getFields ()) {
					// set method
					writer.write (Lang.TAB + JavaSpec.Public + JavaSpec.Void + Lang.SPACE + 
						JavaSpec.Set + capitalize (field.getName ()) + Lang.PARENTH_OPEN + field.getType () + Lang.SPACE + field.getName () + Lang.PARENTH_CLOSE + Lang.SPACE + Lang.OBJECT_OPEN + Lang.ENDLN
					);
						writer.write (Lang.TAB + Lang.TAB + JavaSpec.This + Lang.DOT + field.getName () + Lang.EQUALS + field.getName () + Lang.SEMICOLON + Lang.ENDLN);
					writer.write (Lang.TAB + Lang.OBJECT_CLOSE + Lang.ENDLN);
					
					// get method
					writer.write (Lang.TAB + JavaSpec.Public + field.getType () + Lang.SPACE + 
						JavaSpec.Get + capitalize (field.getName ()) + Lang.PARENTH_OPEN + Lang.PARENTH_CLOSE + Lang.SPACE + Lang.OBJECT_OPEN + Lang.ENDLN
					);
						writer.write (Lang.TAB + Lang.TAB + JavaSpec.Return + Lang.SPACE + field.getName () + Lang.SEMICOLON + Lang.ENDLN);
					writer.write (Lang.TAB + Lang.OBJECT_CLOSE + Lang.ENDLN);
				}
				
				// class declaration - end
				writer.write (Lang.OBJECT_CLOSE);
				
			} finally {
				IOUtils.closeQuietly (writer);
			}
		}
		
	}
	
	private static String type (String type) {
		String fType = Types.get (type.toLowerCase ());
		if (fType == null) {
			return type;
		}
		return fType;
	}
	
	private static String capitalize (String name) {
		return name.substring (0, 1).toUpperCase () + name.substring (1);
	}

}

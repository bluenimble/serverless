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
package com.bluenimble.platform.icli.mgm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.cli.InstallI18nException;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolClient;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.ToolStartupException;
import com.bluenimble.platform.cli.command.Command;
import com.bluenimble.platform.cli.impls.JLineTool;
import com.bluenimble.platform.cli.impls.ToolContextImpl;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.icli.mgm.boot.BnMgmICli;
import com.bluenimble.platform.icli.mgm.boot.CliUtils;
import com.bluenimble.platform.icli.mgm.boot.Spec;
import com.bluenimble.platform.icli.mgm.commands.dev.ApiCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.CreateCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.FeaturesCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.HttpCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.KeysCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.LoadCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.SecureCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.UseCommand;
import com.bluenimble.platform.icli.mgm.commands.dev.WorkspaceCommand;
import com.bluenimble.platform.icli.mgm.commands.mgm.MacroSourceCommand;
import com.bluenimble.platform.icli.mgm.commands.mgm.RemoteCommand;
import com.bluenimble.platform.icli.mgm.commands.mgm.ScriptSourceCommand;
import com.bluenimble.platform.icli.mgm.monitors.KeysMonitor;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class BlueNimble extends JLineTool {
	
	private static final long serialVersionUID = -6945317797511551981L;
	
	public static final String	JsonExt		= ".json";
	public static final String	ScriptExt	= ".sc";
	public static final String	MacroExt	= ".js";

	public static final String ResponseVarName	= "__ResponseVarName__";
	public static final String GlobalContext	= "global";
	
	public interface DefaultVars {
		String Endpoint 		= "endpoint";
		String StorageProvider 	= "storage.provider";
		String DatabaseProvider = "database.provider";
		String CacheProvider 	= "cache.provider";
		String QueryAll 		= "q.all";
		String QueryCount 		= "q.count";
		String ServiceTemplate 	= "service.template";
		String ApiTemplate 		= "api.template";
		String ApiSecurityEnabled 		
								= "api.security.enabled";
		String Paraphrase 		= "paraphrase";
		String SslTrust 		= "ssl.trust";
		String RemoteHeadersAccept 		
								= "remote.headers.accept";
		String SpecLanguage 	= "spec.lang";
		
		String UserMeta			= "user.meta";
			String UserName		= "name";
			String UserPackage	= "package";
	}
	
	public interface SpecLangs {
		String Json = "json";
		String Yaml = "yaml";
	}
	
	public static File 			Home;
	public static File 			Work;
	
	public static File 			Workspace;
	
	public static File 			ConfigFile;
	public static JsonObject 	Config;
	
	public static JsonObject 	version;
	
	private static final Map<String, Keys> KeysMap = new HashMap<String, Keys> ();
	
	private static Keys currentKeys;
	
	public BlueNimble (File home) throws InstallI18nException, ToolStartupException {
		super ();
		
		addCommand (new WorkspaceCommand ());

		addCommand (new KeysCommand ());
		addCommand (new LoadCommand ());
		
		addCommand (new WorkspaceCommand ());
		addCommand (new ApiCommand ());
		addCommand (new CreateCommand ());
		addCommand (new SecureCommand ());
		addCommand (new UseCommand ());
		
		addCommand (new FeaturesCommand ());
		
		addCommand (new HttpCommand ());

		Home = home;
		
		// create bluenimble working home
		Work = new File (new File (System.getProperty ("user.home")), "bluenimble");
		if (!Work.exists ()) {
			Work.mkdirs ();
		}
		
		ConfigFile = new File (Work, CliSpec.CliConfig);
		if (ConfigFile.exists ()) {
			try {
				Config = Json.load (ConfigFile);
			} catch (Exception e) {
				throw new ToolStartupException (e.getMessage (), e);
			}
			
			String sWorkspace = Json.getString (Config, CliSpec.Config.Workspace);
			if (!Lang.isNullOrEmpty (sWorkspace)) {
				File workspace = new File (sWorkspace);
				if (workspace.exists () && workspace.isDirectory ()) {
					Workspace = workspace;
					try {
						loadApis (workspace);
					} catch (Exception e) {
						throw new ToolStartupException (e.getMessage (), e);
					}
				}
			} else {
				Config.remove (CliSpec.Config.Workspace);
			}
		}
		
		if (Config == null) {
			Config = new JsonObject ();
		}
		
		// set variables from config
		loadVariables ();

		if (Workspace == null) {
			Workspace = new File (Work, "workspace");
			Workspace.mkdir ();
			Config.set (CliSpec.Config.Workspace, Workspace.getAbsolutePath ());
		}
		
		try {
			Json.store (Config, ConfigFile);
		} catch (Exception e) {
			throw new ToolStartupException (e.getMessage (), e);
		}
		
		// load commands
		loadCommands (new File (Home, "commands"));
		loadCommands (new File (Work, "commands"));
		
		// load scripts
		loadScripts (new File (Home, "scripts"));
		loadScripts (new File (Work, "scripts"));
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		if (!vars.containsKey (Tool.ParaPhraseVar)) {
			try {
				processCommand ("set paraphrase serverless");
			} catch (Exception e) {
				System.out.println ("ERROR: Can't set default paraphrase. Cause: " + e.getMessage ());
			}			
		}
		
		loadKeys (this);
		
		try {
			new KeysMonitor (1000).start (BlueNimble.this);
		} catch (Exception e) {
			System.out.println ("ERROR: Can't start monitors. Cause: " + e.getMessage ());
		}
	}
	
	private static void loadApis (File workspace) throws Exception {
		JsonObject oApis = new JsonObject ();
		File [] apis = workspace.listFiles (new FileFilter () {
			@Override
			public boolean accept (File f) {
				return f.isDirectory ();
			}
		});
		if (apis != null && apis.length > 0) {
			for (File api : apis) {
				readApi (api, oApis);
			}
		}
		if (oApis.isEmpty ()) {
			Config.remove (CliSpec.Config.Apis);
		} else {
			Config.set (CliSpec.Config.Apis, oApis);
		}
		String currentApi = Json.getString (Config, CliSpec.Config.CurrentApi);
		if (Lang.isNullOrEmpty (currentApi)) {
			return;
		}
		if (Lang.isNullOrEmpty (Json.getString (oApis, currentApi))) {
			Config.remove (CliSpec.Config.CurrentApi);
		}
		
		if (!oApis.isEmpty ()) {
			int size = oApis.count ();
			int counter = 0;
			Iterator<String> namespaces = oApis.keys ();
			while (namespaces.hasNext ()) {
				String apiNs = namespaces.next ();
				counter++;
				if (counter == size) {
					Config.set (CliSpec.Config.CurrentApi, apiNs);
					saveConfig ();
				}
			}
		}
	}

	private static void readApi (File api, JsonObject oApis) {
		String namespace = loadApi (api);
		if (namespace != null) {
			oApis.put (namespace, api.getName ());
		}
	}
	
	public static String loadApi (File api) {
		File descriptor = new File (api, "api.json");
		if (!descriptor.exists () || !descriptor.isFile ()) {
			return null;
		}
		JsonObject oDescriptor = null;
		try {
			oDescriptor = Json.load (descriptor);
		} catch (Exception e) {
			// igmore
		}
		if (oDescriptor == null) {
			return null;
		}
		String namespace = Json.getString (oDescriptor, Api.Spec.Namespace);
		if (Lang.isNullOrEmpty (namespace)) {
			return null;
		}
		return namespace;
	}
	
	@Override
	public String getName () {
		return Json.getString (BnMgmICli.Software, Spec.Name);
	}

	@Override
	public String getDescription () {
		return 	Json.getString (BnMgmICli.Software, Spec.Title) + Lang.ENDLN + Lang.ENDLN + Json.getString (BnMgmICli.Software, Spec.Copyright) + 
				Lang.ENDLN + Lang.ENDLN + "Version " + CliUtils.sVersion (Home);
	}
	
	@Override
	public boolean isAllowed () {
		return true;
	}

	@Override
	public ToolClient getClient () {
		return new ToolClient () {
			private static final long serialVersionUID = 2372898209586150433L;
			@Override
			public String getId () {
				return Json.getString (BnMgmICli.Software, Spec.Name);
			}
			@Override
			public String getName () {
				return Json.getString (BnMgmICli.Software, Spec.Name);
			}
		};
	}
	
	@Override
	public void onReady () {
		String sWorkspace = Json.getString (Config, CliSpec.Config.Workspace);
		if (Lang.isNullOrEmpty (sWorkspace)) {
			printer ().content ("__PS__ GREEN:Current workspace", "No workspace found! it seems it's your first time running BlueNimble.\nUse command: ws [path to a workspace folder]. Ex: ws /home/me/myprojects/bluenimble");
		} else {
			printer ().content ("__PS__ GREEN:Current workspace", sWorkspace);
		}
		printer ().content ("__PS__ GREEN:Keys folder", keysFolder ().getAbsolutePath ());
	}
	
	public static void saveConfig () throws IOException {
		Json.store (Config, ConfigFile);
	}
	
	public static void workspace (File workspace) throws Exception {
		Workspace = workspace;
		loadApis (workspace);
		Config.set (CliSpec.Config.Workspace, workspace.getAbsolutePath ());
		saveConfig ();
	}
	
	public static Keys keys (String name) {
		return KeysMap.get (name);
	}
	
	public static Keys keys () {
		return currentKeys;
	}
	
	public static Map<String, Keys> allKeys () {
		return KeysMap;
	}
	
	public static Keys useKeys (String name) throws Exception {
		name = name.toLowerCase ();
		Keys s = KeysMap.get (name);
		if (s == null) {
			throw new Exception ("\n  keys " + name + " not found.\n\n  use 'create keys [yours keys file]' to load keys in bluenimble sdk. Ex. create keys my-app-prod.keys");
		}
		currentKeys = s;
		
		Config.set (CliSpec.Config.CurrentKeys, name);
		saveConfig ();
		
		return keys ();
	}
	
	public static File keysFolder () {
		File keysFolder = new File (Work, CliSpec.Config.Keys);
		
		String secretPath = Json.getString (Config, CliSpec.Config.Keys);
		if (!Lang.isNullOrEmpty (secretPath)) {
			keysFolder = new File (secretPath);
		}
		return keysFolder;
	}
	
	private void loadCommands (File commands) {
		if (!commands.exists () || !commands.isDirectory ()) {
			commands.mkdir ();
			return;
		}
		File [] folders = commands.listFiles (new FileFilter () {
			@Override
			public boolean accept (File f) {
				return f.isDirectory ();
			}
		});
		for (File fld : folders) {
			String ctx = fld.getName ();
			if (GlobalContext.equals (ctx)) {
				ctx = Tool.ROOT_CTX;
			} else {
				ToolContext toolContext = getContext (ctx);
				if (toolContext == null) {
					addContext (new ToolContextImpl (ctx));
				}
			}
			File [] files = fld.listFiles (new FileFilter () {
				@Override
				public boolean accept (File f) {
					return f.isFile () && f.getName ().endsWith (".json");
				}
			});
			String name = null;
			for (File f : files) {
				name = f.getName ().substring (0, f.getName ().indexOf (JsonExt));
				try {
					addCommand (new RemoteCommand (ctx, name, Json.load (f)));
				} catch (Exception e) {
					System.out.println ("ERROR: Can't load command " + name + ". Cause: " + e.getMessage ());
				}
			}
		}
	}
	
	private void loadScripts (File scripts) {
		if (!scripts.exists () || !scripts.isDirectory ()) {
			scripts.mkdir ();
			return;
		}
		File [] folders = scripts.listFiles (new FileFilter () {
			@Override
			public boolean accept (File f) {
				return f.isDirectory ();
			}
		});
		for (File fld : folders) {
			String ctx = fld.getName ();
			if (GlobalContext.equals (ctx)) {
				ctx = Tool.ROOT_CTX;
			} else {
				ToolContext toolContext = getContext (ctx);
				if (toolContext == null) {
					addContext (new ToolContextImpl (ctx));
				}
			}
			File [] files = fld.listFiles (new FileFilter () {
				@Override
				public boolean accept (File f) {
					return f.isFile () && (f.getName ().endsWith (MacroExt) || f.getName ().endsWith (ScriptExt));
				}
			});
			String name = null;
			for (File f : files) {
				name = f.getName ().substring (0, f.getName ().lastIndexOf (Lang.DOT));
				try {
					Command c = null;
					if (f.getName ().endsWith (MacroExt)) {
						c = new MacroSourceCommand (ctx, name, f);
					} else if (f.getName ().endsWith (ScriptExt)) {
						c = new ScriptSourceCommand (ctx, name, f);
					}
					if (c != null) {
						addCommand (c);
					}
				} catch (Exception e) {
					System.out.println ("ERROR: Can't load command " + name + ". Cause: " + e.getMessage ());
				}
			}
		}
	}
	
	public static void loadKeys (Tool tool) throws ToolStartupException {
		File keysFolder = keysFolder ();
		
		if (!keysFolder.exists ()) {
			keysFolder.mkdirs ();
		}
		
		if (!keysFolder.isDirectory ()) {
			throw new ToolStartupException (keysFolder.getAbsolutePath () + " isn't valid forder to store bluenimble keys");
		}
		
		String sCurrentKeys = Json.getString (Config, CliSpec.Config.CurrentKeys);
		
		File [] keys = keysFolder.listFiles (new FileFilter () {
			@Override
			public boolean accept (File f) {
				return f.isFile () && f.getName ().endsWith (CliSpec.KeysExt);
			}
		});
		if (keys == null || keys.length == 0) {
			Config.remove (CliSpec.Config.CurrentKeys);
			return;
		}
		
		for (File s : keys) {
			loadKeys (tool, s, sCurrentKeys);
		}
	}
	
	private static String loadKeys (Tool tool, File s, String currentSecret) throws ToolStartupException {
		String sname = s.getName ().substring (0, s.getName ().lastIndexOf (Lang.DOT)).toLowerCase ();
		
		InputStream in = null;
		
		try {
			in = new FileInputStream (s);
			
			Keys keys = new Keys (sname, Json.load (new ByteArrayInputStream (Base64.decodeBase64 (IOUtils.toString (in))), tool.getParaphrase (true)));
			KeysMap.put (sname, keys);
			if (sname.equals (currentSecret)) {
				useKeys (sname);
			}
		} catch (Exception e) {
			tool.printer ().error ("Can't load keys " + s.getName () + ". Cause: " + e.getMessage ());
		} finally {
			IOUtils.closeQuietly (in);
		}
		return sname;
	}
	
	public static String loadKeys (Tool tool, File s) throws ToolStartupException {
		return loadKeys (tool, s, null);
	}
	
	@Override
	public void saveVariable (String name, Object value) throws IOException {
		
		String [] varsNames = null;
		
		if (DefaultVars.ApiTemplate.equals (name)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> vars = (Map<String, Object>)getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
			if (value == null) {
				vars.remove (DefaultVars.ServiceTemplate);
			} else {
				vars.put (DefaultVars.ServiceTemplate, value);
			}
			varsNames = new String [] {DefaultVars.ApiTemplate, DefaultVars.ServiceTemplate};
		} else {
			varsNames = new String [] { name };
		}
		
		JsonObject oVars = Json.getObject (Config, CliSpec.Config.Variables);
		
		if (oVars == null) {
			return;
		}
		
		for (String vn : varsNames) {
			if (value == null) {
				oVars.remove (vn);
			} else {
				oVars.set (vn, value);
			}
		}
		
		saveConfig ();	
	}

	private void loadVariables () {
		JsonObject oVars = Json.getObject (Config, CliSpec.Config.Variables);
		if (oVars == null) {
			oVars = new JsonObject ();
			Config.set (CliSpec.Config.Variables, oVars);
		}
		if (!oVars.containsKey (DefaultVars.UserMeta)) {
			oVars.set (DefaultVars.UserMeta, new JsonObject ().set (DefaultVars.UserName, System.getProperty ("user.name")));
		}
		if (!oVars.containsKey (DefaultVars.StorageProvider)) {
			oVars.set (DefaultVars.StorageProvider, "default");
		}
		if (!oVars.containsKey (DefaultVars.DatabaseProvider)) {
			oVars.set (DefaultVars.DatabaseProvider, "default");
		}
		if (!oVars.containsKey (DefaultVars.CacheProvider)) {
			oVars.set (DefaultVars.CacheProvider, "default");
		}
		if (!oVars.containsKey (DefaultVars.QueryAll)) {
			oVars.set (DefaultVars.QueryAll, new JsonObject ().set ("where", new JsonObject ()));
		}
		if (!oVars.containsKey (DefaultVars.QueryCount)) {
			oVars.set (DefaultVars.QueryCount, new JsonObject ().set ("where", new JsonObject ()).set ("select", new JsonArray ().set (null, "count(1)")));
		}
		if (!oVars.containsKey (DefaultVars.ServiceTemplate)) {
			oVars.set (DefaultVars.ServiceTemplate, "database/javascript");
		}
		if (!oVars.containsKey (DefaultVars.ApiTemplate)) {
			oVars.set (DefaultVars.ApiTemplate, "database/javascript");
		}
		if (!oVars.containsKey (DefaultVars.SpecLanguage)) {
			oVars.set (DefaultVars.SpecLanguage, SpecLangs.Json);
		}
		if (oVars.containsKey (DefaultVars.Paraphrase)) {
			try {
				super.setParaphrase (Json.getString (oVars, DefaultVars.Paraphrase), false);
			} catch (Exception e) {
				printer ().error ("Can't read user paraphrase. Cause: " + e.getMessage ());
			}
		}
		if (!oVars.containsKey (DefaultVars.SslTrust)) {
			oVars.set (DefaultVars.SslTrust, "true");
		}
		if (!oVars.containsKey (DefaultVars.ApiSecurityEnabled)) {
			oVars.set (DefaultVars.ApiSecurityEnabled, "true");
		}
		if (!oVars.containsKey (DefaultVars.RemoteHeadersAccept)) {
			oVars.set (DefaultVars.RemoteHeadersAccept, ApiContentTypes.Json);
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		for (Object key : oVars.keySet ()) {
			vars.put (String.valueOf (key), oVars.get (key));
		}
		
	}
	
	@Override
	public void setParaphrase (String paraphrase, boolean encrypt) throws Exception {
		super.setParaphrase (paraphrase, encrypt);
		
		// loop over keys, encrypt
		if (KeysMap == null || KeysMap.isEmpty ()) {
			return;
		}
		
		Iterator<String> names = KeysMap.keySet ().iterator ();
		while (names.hasNext ()) {
			String k = names.next ();
			updateKeys (KeysMap.get (k), paraphrase);
		}
		
	}

	private void updateKeys (Keys keys, String paraphrase) throws Exception {
		Json.store (keys.json (), new File (keysFolder (), keys.alias () + CliSpec.KeysExt), paraphrase, true);
	}
	
}

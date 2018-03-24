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
package com.bluenimble.platform.icli.mgm.commands.dev.impls;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.CliSpec.Templates;
import com.bluenimble.platform.icli.mgm.utils.CodeGenUtils;
import com.bluenimble.platform.icli.mgm.utils.SpecUtils;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.json.printers.YamlStringPrinter;

public class CreateApiHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	private static final String DefaultTemplate = "blank/javascript";
	private static final String Namespace 	= "namespace";
	
	private static final CommandHandler SecureApiHandler = new SecureApiHandler ();

	@SuppressWarnings("unchecked")
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("api namespace required. ex. create api myapi");
		}
		
		String namespace 	= args [0];
		if (!Pattern.matches ("^[a-zA-Z0-9_-]*$", namespace)) {
			throw new CommandExecutionException ("api namespace must contain only numbers, letters, '_' and '-'");
		}
		
		String sApiFolder 	= namespace;
		if (args.length > 1) {
			sApiFolder = args [1];
		}
		
		File apiFolder = new File (BlueNimble.Workspace, sApiFolder);
		if (apiFolder.exists ()) {
			try {
				tool.printer ().info (sApiFolder + " already exists");
				String exists = Json.getString (Json.getObject (BlueNimble.Config, CliSpec.Config.Apis), namespace);
				if (!Lang.isNullOrEmpty (exists)) {
					return null;
				}
				tool.printer ().info ("binding to the current workspace");
				String ns = BlueNimble.loadApi (apiFolder);
				if (ns == null) {
					tool.printer ().warning ("unable to bind current api. Please check if the api.json exists and the namespace is there.");
				}
				Json.getObject (BlueNimble.Config, CliSpec.Config.Apis).set (ns, sApiFolder);
				BlueNimble.Config.set (CliSpec.Config.CurrentApi, sApiFolder);
			} catch (Exception ex) {
				throw new CommandExecutionException (ex.getMessage (), ex);
			}
		}
		
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);

		String template 	= (String)vars.get (BlueNimble.DefaultVars.TemplateApi);
		if (Lang.isNullOrEmpty (template)) {
			template = DefaultTemplate;
		}
		
		File fTemplate = new File (new File (BlueNimble.Home, Templates.class.getSimpleName ().toLowerCase () + Lang.SLASH + Templates.Apis), template);
		if (!fTemplate.exists ()) {
			throw new CommandExecutionException ("api template '" + template + "' not installed");
		}
		
		apiFolder.mkdir ();
		
		try {
			FileUtils.copy (fTemplate, apiFolder, false);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		
		String specLang 	= (String)vars.get (BlueNimble.DefaultVars.SpecLanguage);
		if (Lang.isNullOrEmpty (specLang)) {
			specLang = BlueNimble.SpecLangs.Json;
		}
		
		CodeGenUtils.writeAll (apiFolder, (JsonObject)new JsonObject ().set (Namespace, namespace), specLang);
		
		BlueNimble.Config.set (CliSpec.Config.CurrentApi, namespace);
		JsonObject oApis = Json.getObject (BlueNimble.Config, CliSpec.Config.Apis);
		if (oApis == null) {
			oApis = new JsonObject ();
			BlueNimble.Config.set (CliSpec.Config.Apis, oApis);
		}
		oApis.set (namespace, sApiFolder);
		
		boolean secure = false;

		String sSecure 	= (String)vars.get (BlueNimble.DefaultVars.ApiSecurityEnabled);
		if (Lang.isNullOrEmpty (sSecure)) {
			secure = false;
		} else {
			secure = Lang.TrueValues.contains (sSecure.trim ().toLowerCase ());
		}
		
		try {
			JsonObject apiSpec = SpecUtils.read (apiFolder);
			
			JsonObject codeGen = Json.getObject (apiSpec, "_codegen_");
			if (codeGen != null) {
				secure = Json.getBoolean (codeGen, "secure", true);
				apiSpec.remove ("_codegen_");
				SpecUtils.write (apiFolder, apiSpec);
			}
			
			BlueNimble.saveConfig ();
			
			tool.printer ().content (
				"Api '" + namespace + "' created! path: $ws/ " + sApiFolder, 
				BlueNimble.SpecLangs.Json.equals (specLang) ? apiSpec.toString (2) : new YamlStringPrinter (2).print (apiSpec).toString ()
			);
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		}
		
		if (secure) {
			SecureApiHandler.execute (tool, new String [] { namespace, "token+signature", Lang.STAR });
		}

		return new DefaultCommandResult (CommandResult.OK, null);
	}

	@Override
	public String getName () {
		return "api";
	}

	@Override
	public String getDescription () {
		return "create an api project 'create api travel'";
	}

	@Override
	public Arg [] getArgs () {
		return new Arg [] {
				new AbstractArg () {
					@Override
					public String name () {
						return "namespace";
					}
					@Override
					public String desc () {
						return "api namespace. In general, it's your application name";
					}
				},
				new AbstractArg () {
					@Override
					public String name () {
						return "folder";
					}
					@Override
					public String desc () {
						return "folder name. By default, the api folder will be the same as the api namespace and it will be created under your workspace";
					}
					@Override
					public boolean required () {
						return false;
					}
				}
		};
	}

}

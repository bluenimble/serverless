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
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.templating.SimpleVariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public class LoadFeatureHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	private static final String VarsSep 	= "__";
	
	private static final String Templates 	= "templates/features";
	
	private static final String Name 		= "name";
	private static final String Provider 	= "provider";
	private static final String Default 	= "default";

	
	private static final DefaultExpressionCompiler ExpressionCompiler = new DefaultExpressionCompiler ();
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("feature file path required. ex. load feature aTempate.json");
		}
		
		String path = args [0];
		
		File features = new File (BlueNimble.Home, Templates);
		
		File templateFile = new File (features, path + ".json");
		
		if (!templateFile.exists () || !templateFile.isFile ()) {
			throw new CommandExecutionException ("invalid file path > " + path);
		}
		
		String name = templateFile.getName ();
		
		String fKey = name.substring (0, name.lastIndexOf (Lang.DOT));
		
		JsonObject oFeature;
		try {
			oFeature = Json.load (templateFile);
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		}
		
		String provider 	= fKey;
		String [] variables	= null;
		
		int indexOfDash = fKey.indexOf (Lang.DASH);
		if (indexOfDash > 0) {
			provider 	= fKey.substring (0, indexOfDash);
			variables 	= Lang.split (fKey.substring (indexOfDash + 1), VarsSep, true);
		}
		
		if (!oFeature.containsKey (Name)) {
			oFeature.set (Name, Default);
		}
		if (!oFeature.containsKey (Provider)) {
			oFeature.set (Provider, provider);
		}
		
		final Map<String, String> mVariables = new HashMap<String, String> ();
		
		if (variables != null && variables.length > 0) {
			for (String v : variables) {
				String vKey 	= v.substring (0, v.indexOf (Lang.UNDERSCORE));
				String vValue 	= v.substring (v.indexOf (Lang.UNDERSCORE) + 1);
				mVariables.put (vKey, vValue);
			}
		}
		
		if (mVariables != null) {
			oFeature = (JsonObject)Json.resolve (oFeature, ExpressionCompiler, new SimpleVariableResolver () {
				private static final long serialVersionUID = 1L;

				@Override
				public Object resolve (String namespace, String... property) {
					return mVariables.get (Lang.join (property, Lang.DOT));
				}
			});
		}
		
		oFeature.shrink ();
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.currentContext ().get (ToolContext.VARS);
		vars.put (fKey, oFeature);
		
		return new DefaultCommandResult (CommandResult.OK, oFeature);
	}


	@Override
	public String getName () {
		return "feature";
	}

	@Override
	public String getDescription () {
		return "load a feature template";
	}
	
	@Override
	public Arg [] getArgs () {
		return new Arg [] {
			new AbstractArg () {
				@Override
				public String name () {
					return "name";
				}
				@Override
				public String desc () {
					return "feature template file name";
				}
			}
		};
	}

}

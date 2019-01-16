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

// native imports 
var System 			= native ('java.lang.System');
var File 			= native ('java.io.File');
var Pattern 		= native ('java.util.regex.Pattern');

var Lang 			= native ('com.bluenimble.platform.Lang');
var Api 			= native ('com.bluenimble.platform.api.Api');

var JsonObject 		= native ('com.bluenimble.platform.json.JsonObject');
var JsonArray 		= native ('com.bluenimble.platform.json.JsonArray');

var FileUtils 		= native ('com.bluenimble.platform.FileUtils');
var Json 			= native ('com.bluenimble.platform.Json');
var Yamler 			= native ('com.bluenimble.platform.icli.mgm.utils.Yamler');

var BuildUtils 		= native ('com.bluenimble.platform.icli.mgm.utils.BuildUtils');
var SpecUtils 		= native ('com.bluenimble.platform.icli.mgm.utils.SpecUtils');
var OsCommander 	= native ('com.bluenimble.platform.icli.mgm.utils.OsCommander');

var BlueNimble		= native ('com.bluenimble.platform.icli.mgm.BlueNimble');

var Verbs = {
	get: 'Get',
	post: 'Create',
	put: 'Update',
	'delete': 'Delete',
	patch: 'Patch',
	head: 'Echo',
	options: 'Info'
};
								  
function extract (service, file, script) {
	var markers = FileUtils.readStartsWith (script, '//@', true);
	if (markers.isEmpty ()) {
		return;
	}
	
	Tool.note ('\t Found ' + markers.size () + ' markers in ' + script.getName ());
	
	for (var i = 0; i < markers.size (); i++) {		
		// read marker and add to service spec
		var marker = markers.get (i);
		
		var line 	= marker.substring (0, marker.indexOf (' '));
		
		marker = marker.substring (marker.indexOf (' ') + 4).trim ();
		
		if (!service.markers) {
			service.markers = new JsonArray ();
		}
		
		var indexOfLt = marker.indexOf ('<');
		var indexOfGt = marker.indexOf ('>');
		
		if (indexOfLt == 0 && indexOfGt > 1) {
			var oMarker = new JsonObject ();
			oMarker.line = line;
			service.markers.add (oMarker);
			
			oMarker.type = marker.substring (indexOfLt + 1, indexOfGt).toLowerCase ();
			var line = marker.substring (indexOfGt + 1).trim ();
			if (line == '') {
				continue;
			}
			var properties = Lang.split (line, '|', true);
			for (var j = 0; j < properties.length; j++) {
				var property = properties [j];
				var pv = Lang.split (property, ' ', true);
				oMarker [ pv [0] ] = (pv.length > 1 ? pv [1] : true); 
			}
		} else {
			if (service.markers.isEmpty ()) {
				continue;
			}
			marker = marker.trim ();
			if (marker.endsWith ('/')) {
				marker = marker.substring (0, marker.length () - 1);
			} else {
				marker += '\n';
			}
			var lastMarker = service.markers.get (service.markers.size () - 1);
			if (lastMarker.comment) {
				lastMarker.comment += marker;
			} else {
				lastMarker.comment = marker;
			}
		}
	}

}

function saveService (service, file) {
	if (!service.verb) {
		service.verb = 'get';
	}
	
	// multiple verbs per service
	if (service.verb instanceof JsonArray) {
		for (var i = 0; i < service.verb.length; i++) {
			var duplicata = service.duplicate ();
			duplicata.verb = service.verb.get (i).toLowerCase ();
			var action = Verbs [duplicata.verb];
			if (!action) {
				action = duplicata.verb.substring (0, 1).toUpperCase () + duplicata.verb.substring (1);
			}
			// store new spec
			Json.store (duplicata, new File (file.getParentFile (),  action + file.getName ()), true);
            
            // delete original
            file.delete ();
            
		}
	} else {
		// store new spec
		Json.store (service, file, true);
	}
};

function validate (apiFolder, folderOrFile, transformData) {

	if (folderOrFile.getName ().startsWith ('.')) {
		return;
	}

	if (folderOrFile.isDirectory ()) {
		var files = folderOrFile.listFiles ();
		for (var i = 0; i < files.length; i++) {
			validate (apiFolder, files [i], transformData);
		}
		return;
	}
	
	// load service spec, find script and extract issues
	var serviceName = folderOrFile.getName ();
	serviceName = serviceName.substring (0, serviceName.lastIndexOf ('.'));
	// Tool.note ('\t Validating service spec ' + ' ' + serviceName);
	var service;
	try {
		service = Json.load (folderOrFile);
	} catch (ex) {
		Tool.error ('File ' + folderOrFile.getAbsolutePath () + ' -> ' + ex.message);
	}
	if (!service) {
		return;
	}
	
	// transform
	service = BuildUtils.transform (service, transformData);
	
	// add prefix if any
	if (transformData.R.api && transformData.R.api.prefix) {
		service.endpoint = transformData.R.api.prefix + service.endpoint;
	}
	
	// apply services transform
	if (transformData.R.api && transformData.R.api.services && transformData.R.api.services.length > 0) {
		for (var i = 0; i < transformData.R.api.services.length; i++) {
			var oMatch = transformData.R.api.services [i];
			var wildcard = oMatch.matches;
			var matches = true;
			if (wildcard) {
				matches = Lang.wmatches (wildcard, service.endpoint);
			}
			if (matches && oMatch.apply && (oMatch.apply instanceof JsonObject)) {
				service.merge (oMatch.apply);
			}
		}
	}
	
	// parse function code if any
	if (!service.runtime || !service.runtime.function) {
		saveService (service, folderOrFile);
		return;
	}
	
	var _function = new File (apiFolder, 'resources/' + service.runtime.function);
	if (!_function.exists ()) {
        saveService (service, folderOrFile);
		Tool.error ('Service spec has a reference to function ' + script.getAbsolutePath () + ' which is not found');
		return;
	}
	
	extract (service, folderOrFile, _function);
	
	saveService (service, folderOrFile);
	
	return;
	
}

// Start Of Script
var startTime = System.currentTimeMillis ();

// check if valid command args
if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. push api [ApiNs required] [Target optional]';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 2) {
	throw 'missing command arguments. eg. push api ApiNs OR push api ApiNs prod';
}

if (tokens [0] != 'api') {
	throw 'wrong push target object. eg. push api ApiNs';
}

// api namespace
var apiNs = tokens [1];

var apiSrcPath = Config.apis [apiNs];
if (!apiSrcPath) {
	apiSrcPath = apiNs;
}

Vars ['ApiHome'] = Config.workspace + '/' + apiSrcPath;

var recipe = new JsonObject ();

// load recipes
var allRecipes;
var isYaml = false;

var recipesFolder = Home;
if (Vars ['recipes.folder']) {
	recipesFolder = new File (Vars ['recipes.folder']);
	if (!recipesFolder.exists ()) {
		throw Vars ['recipes.folder'] + " isn't a valid recipes folder. Make sure to set recipes.folder variable and a recipes.json/yaml is present";
	}
}

var fRecipes = new File (recipesFolder, 'recipes.json');
if (fRecipes.exists ()) {
	allRecipes = Json.load (fRecipes);
} else {
	fRecipes = new File (Home, 'recipes.yaml');
	if (fRecipes.exists ()) {
		isYaml = true;
		allRecipes = Yamler.load (fRecipes);
	}
}

var rId;
var mergeCommon = true;

var targetRecipe;

if (tokens.length > 2) {
	rId = tokens [2];
	if (!allRecipes) {
		throw 'No recipes.json nor recipes.yaml file exists in your home folder';
	}
	if (!allRecipes.containsKey (rId)) {
		throw 'Recipe ' + rId + ' not found in recipes.' + (isYaml ? 'json' : 'yaml') + ' file';
	}
	
	targetRecipe = allRecipes [rId];

	if (!(targetRecipe instanceof JsonObject)) {
		throw 'Recipe ' + rId + ' is not a valid object';
	}
}

if (allRecipes && allRecipes.common && (allRecipes.common instanceof JsonObject)) {
	mergeCommon = true;
	recipe = allRecipes.common.duplicate ();
}
if (targetRecipe) {
	recipe.merge (targetRecipe);
}

recipe = BuildUtils.transform (recipe, Vars);

// Set adt. build vars
var backVars = {};

if (recipe.vars) {
	for (var k in vars) {
		var old = Vars [k];
		backVars [k] = (typeof old != 'undefined' ? old : '__REMOVE__');
		Vars [k] = vars [k];
	}
}

var transformData = new JsonObject ().set ("R", recipe);

// Create the build folder if it doesn't exist
var buildFolder = new File (Home, 'build');
if (!buildFolder.exists ()) {
	buildFolder.mkdir ();
}

var apiFolder = new File (buildFolder, apiNs);

if (!Vars ['build.release.nocopy'] || Vars ['build.release.nocopy'] != 'true') {
	// delete api folder
	FileUtils.delete (apiFolder);
	
	// copy
	var apiSrc = new File (Config.workspace + '/' + apiSrcPath);
	// mavenized project
	if (new File (apiSrc, "pom.xml").exists ()) {
		// run maven
		var apiMvnBuild = new File (apiSrc, "build");
		if (!apiMvnBuild.exists ()) {
			throw apiMvnBuild.getAbsolutePath () + ' not found';
		}
		
		Tool.note ('Building api - (mvn clean install) ' + apiNs);

		OsCommander.execute (
			Tool,
			apiSrc, 
			"mvn clean install",
			null
		);
		
		var apiMvnBuildFiles = apiMvnBuild.listFiles ();
		if (apiMvnBuildFiles == null || apiMvnBuildFiles.legth == 0) {
			throw 'api build folder not found. run maven first. mvn clean install';
		}
		if (apiMvnBuildFiles.legth > 1) {
			throw 'there is more than 1 api in the build folder';
		}
		apiSrc = apiMvnBuildFiles [0];
	}
	
	FileUtils.copy (apiSrc, buildFolder, true);
	
	apiFolder = new File (buildFolder, apiSrc.getName ());
	
	// delete unwanted folders
	if (recipe.unwanted) {
		for (var i = 0; i < recipe.unwanted.length; i++) {
			var f = new File (apiFolder, recipe.unwanted [i]); 
			if (f.exists ()) {
				FileUtils.delete (f);
			}
		}
	}
	
	// convert yaml to json
	SpecUtils.y2j (apiFolder, true);
}

Tool.info ('Validating api ' + apiNs);

// read api spec and set installDate, version, user id and stamp
var apiSpecFile = new File (apiFolder, 'api.json');
if (!apiSpecFile.exists ()) {
	throw 'api ' + apiNs + ' not found';
}

var apiSpec = Json.load (apiSpecFile);

var release = new JsonObject ();
apiSpec.set (Api.Spec.Release, release);

// set install date
release.set ("date", Lang.utc ());

// set version (this will change the api namespace) from apiNs to apiNs-version.
// eg travel-v1
if (recipe.version) {
	apiSpec.set (Api.Spec.Name, apiSpec.get (Api.Spec.Name) + " ( " + recipe.version + " )");
}

var Keys;

if (recipe.get ('keys')) {
    Keys = BlueNimble.keys (recipe.get ('keys'));
	if (!Keys) {
		throw 'Security Keys (' + recipe.get ('keys') + ') not found in recipe.';
	}
} else {
	Keys = BlueNimble.keys ();
} 

if (!Keys) {
	throw 'Security Keys not found. Load some keys in order to push your api';
}

Keys = Keys.json ();

// set pusher (user id, email) this is part of the keys
if (Keys.user && Keys.user.id) {
	release.set ('pushedBy', Keys.user.id);
} else if (Vars ['user.meta']) {
	var user = Vars ['user.meta'];
	release.set ('pushedBy', user.id||user.name);
}

// add additional release information from recipe
if (recipe.release) {
	release.merge (recipe.release);
}

if (recipe.api && (recipe.api instanceof JsonObject)) {
	apiSpec.merge (recipe.api);
}

apiSpec = BuildUtils.transform (apiSpec, transformData);

apiNs = apiSpec.namespace;
// validate apiNs
if (!apiNs || apiNs.trim () == '') {
	throw 'api namespace not found';
}
if (!Pattern.matches ("^[a-zA-Z0-9_-]*$", apiNs)) {
	throw 'Invalid api namespace ' + apiNs + '. It should contains only letters, numbers, underscore ( _ ) and dash ( - )';
}

// save api.json file
Json.store (apiSpec, apiSpecFile, true);

// transform, generate, compile and package data models
BuildUtils.generate (apiFolder, Json.find (apiSpec, 'runtime', 'dataModels'), transformData);

// read, validate, transform and extract markers
validate (apiFolder, new File (apiFolder, 'resources/services'), transformData);

Tool.success (apiNs + ' api validated with success');

var newApiFolder = new File (buildFolder, apiNs);

apiFolder.renameTo (newApiFolder);

Tool.content ('__PS__ MAGENTA:Security Keys', Keys.accessKey + ' @ ' + Tool.styled (Keys.endpoints.management, 'yellow') );
var rText = 'None';
if (rId) {
	rText = rId + (mergeCommon ? ' + common' : '');
} else if (mergeCommon) {
	rText = 'common';
}

Tool.content ('__PS__ MAGENTA:Using Recipe', rText);

Tool.command ('echo off');

Tool.info ('Push Api (' + apiNs + ')');
Tool.command ('set api.folder ' + buildFolder.getAbsolutePath ());

Tool.command ('echo on');

if (typeof recipe.install == 'undefined' || recipe.install == null || recipe.install == true) {
	Tool.command ('npush api ' + apiNs);
}

if (recipe.copyTo) {
	var fCopyTo = new File (recipe.copyTo);
	
	var existing = new File (fCopyTo, newApiFolder.getName ());
	if (existing.exists ()) {
		FileUtils.delete (existing);
	}
	
	FileUtils.copy (newApiFolder, fCopyTo, true);
}

if (recipe.run && recipe.run.length > 0) {
	for (var i = 0; i < recipe.run.length; i++) {
		var oRun = recipe.run [i];
		Tool.note ('Execute Commands from directory [' + oRun.directory + ']\n');
		for (var j = 0; j < oRun.commands.length; j++) {
			Tool.note ('RUN | ' + oRun.commands [j]);
			OsCommander.execute (
				Tool,
				new File (oRun.directory),
				oRun.commands [j],
				null
			);
		}
	}
}

Tool.command ('echo off');
Tool.command ('unset api.folder');
Tool.command ('echo on');

FileUtils.delete (newApiFolder);

// Back initial vars
for (var k in backVars) {
	var v = backVars [k];
	if (v == '__REMOVE__') {
		Vars.remove (k);
	} else {
		Vars [k] = v;
	}
}

Tool.info ('Total Push Time: ' + Tool.styled ((System.currentTimeMillis () - startTime), 'yellow') + ' millis');

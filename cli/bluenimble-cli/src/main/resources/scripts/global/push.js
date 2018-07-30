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
	Tool.note ('\t Validating service spec ' + ' ' + serviceName);
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
	
	// parse script
	if (!service.runtime || !service.runtime.function) {
		Json.store (service, folderOrFile);
		return;
	}
	
	var _function = new File (apiFolder, 'resources/' + service.runtime.function);
	if (!_function.exists ()) {
		Tool.error ('Service spec has a reference to function ' + script.getAbsolutePath () + ' which is not found');
		return;
	}
	
	extract (service, folderOrFile, _function);
	
	// store new spec
	Json.store (service, folderOrFile);
	
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

var environment = new JsonObject ();

// load environments
var allEnvs;
var isYaml = false;

var fEnvs = new File (Home, 'environments.json');
if (fEnvs.exists ()) {
	allEnvs = Json.load (fEnvs);
} else {
	fEnvs = new File (Home, 'environments.yaml');
	if (fEnvs.exists ()) {
		isYaml = true;
		allEnvs = Yamler.load (fEnvs);
	}
}

var targetEnv;

if (tokens.length > 2) {
	var envId = tokens [2];
	if (!allEnvs) {
		throw 'No environments.json or .yaml file exists in your home folder';
	}
	if (!allEnvs.containsKey (envId)) {
		throw 'No environment with the id ' + envId + ' found in environments.' + (isYaml ? 'json' : 'yaml') + ' file';
	}
	
	targetEnv = allEnvs [envId];

	if (!(targetEnv instanceof JsonObject)) {
		throw 'Environment with id ' + envId + ' is not a valid object';
	}
}

if (allEnvs && allEnvs.common && (allEnvs.common instanceof JsonObject)) {
	environment = allEnvs.common.duplicate ();
}
if (targetEnv) {
	environment.merge (targetEnv);
}

var transformData = new JsonObject ().set ("Env", environment);

// Create the build folder if it doesn't exist
var buildFolder = new File (Home, 'build');
if (!buildFolder.exists ()) {
	buildFolder.mkdir ();
}

var apiFolder = new File (buildFolder, apiNs);

// ONLY If Requested
if (!Vars ['build.release.nocopy'] || Vars ['build.release.nocopy'] != 'true') {
	// delete api folder
	FileUtils.delete (apiFolder);
	
	// copy
	var apiSrcPath = Config.apis [apiNs];
	if (!apiSrcPath) {
		apiSrcPath = apiNs;
	}
	
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
	
	// assemblies if any
	var assemblies = new File (apiFolder, "assemblies"); 
	if (assemblies.exists ()) {
		FileUtils.delete (assemblies);
	}
	
	// convert yaml to json
	SpecUtils.y2j (apiFolder, true);
}

Tool.note ('Validating api ' + apiNs);

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

// set version (this will change the api namespace) from apiNs to apiNs-version. eg travel-v1
if (environment.version) {
	apiSpec.set (Api.Spec.Name, apiSpec.get (Api.Spec.Name) + " ( " + environment.version + " )");
}

var Keys;

if (environment.get ('keys')) {
    Keys = BlueNimble.keys (environment.get ('keys'));
	if (!Keys) {
		throw 'Security Keys (' + environment.get ('keys') + ') defined in your environments file are not found.';
	}
} else {
	Keys = BlueNimble.keys ();
} 

if (!Keys) {
	throw 'Security Keys not found. Load some keys in order to push your api';
}

Keys = Keys.json ();

Tool.important ('Pushing with keys ' + Keys.accessKey + ' @ ' + Keys.endpoints.management);
Tool.important (environment);

// set pusher (user id, email) this is part of the keys
if (Keys.user && Keys.user.id) {
	release.set ('pushedBy', Keys.user.id);
} else if (Vars ['user.meta']) {
	var user = Vars ['user.meta'];
	release.set ('pushedBy', user.id||user.name);
}

// add additional release information from environment
if (environment.release) {
	release.merge (environment.release);
}

if (environment.api && (environment.api instanceof JsonObject)) {
	apiSpec.merge (environment.api);
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
Json.store (apiSpec, apiSpecFile);

// transform, generate, compile and package data models
BuildUtils.generate (apiFolder, Json.find (apiSpec, 'runtime', 'dataModels'), transformData);

// read, validate, transform and extract markers
validate (apiFolder, new File (apiFolder, 'resources/services'), transformData);

var newApiFolder = new File (buildFolder, apiNs);

apiFolder.renameTo (newApiFolder);

Tool.command ('echo off');

Tool.info ('Push Api (' + apiNs + ')');
Tool.command ('set api.folder ' + buildFolder.getAbsolutePath ());

Tool.command ('echo on');

Tool.command ('npush api ' + apiNs);

Tool.command ('echo off');
Tool.command ('unset api.folder');
Tool.command ('echo on');

FileUtils.delete (newApiFolder);

Tool.info ('Total Push Time: ' + (System.currentTimeMillis () - startTime) + ' millis');

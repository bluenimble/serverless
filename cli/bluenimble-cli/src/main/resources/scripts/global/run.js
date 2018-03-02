/*
	
	TODO: How to get the change log

*/

// native imports 
var System 			= Java.type ('java.lang.System');
var File 			= Java.type ('java.io.File');
var Pattern 		= Java.type ('java.util.regex.Pattern');
var Lang 			= Java.type ('com.bluenimble.platform.Lang');
var FileUtils 		= Java.type ('com.bluenimble.platform.FileUtils');
var Json 			= Java.type ('com.bluenimble.platform.Json');
var JsonObject 		= Java.type ('com.bluenimble.platform.json.JsonObject');
var JsonArray 		= Java.type ('com.bluenimble.platform.json.JsonArray');
var Api 			= Java.type ('com.bluenimble.platform.api.Api');
								  
var startTime = System.currentTimeMillis ();

// check if valid command args
if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. push [ApiNs required] [Version optional]';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 2) {
	throw 'missing command arguments. eg. run namespace [namespace]';
}

if (tokens [0] != 'namespace') {
	throw 'wrong run target object. eg. run namespace [namespace]';
}

// api namespace
var apiNs = tokens [1];

Tool.note ("Building functions namespace '" + apiNs + "'");
Tool.command ('echo off');
Tool.command ('set api.release.nocopy false');
Tool.command ('echo on');

// Create the build folder if it doesn't exist
var buildFolder = new File (Home, 'build');
if (!buildFolder.exists ()) {
	buildFolder.mkdir ();
}

// delete api if exists
var apiFolder = new File (buildFolder, apiNs);
if (apiFolder.exists ()) {
	FileUtils.delete (apiFolder);
}

// copy
FileUtils.copy (new File (Config.workspace + '/' + apiNs), buildFolder, true);

// create the resources / services and scripts folders
new File (apiFolder, 'resources/services').mkdirs ();
new File (apiFolder, 'resources/scripts').mkdirs ();

// rename to api.json and Api.js
new File (apiFolder, 'boot.json').renameTo (new File (apiFolder, 'api.json'));
new File (apiFolder, 'Boot.js').renameTo (new File (apiFolder, 'resources/scripts/Api.js'));

var functions = apiFolder.listFiles ();
for (var i = 0; i < functions.length; i++) {
	var fnFolder = functions [i];
	if (fnFolder.getName () == 'resources' || fnFolder.getName () == 'api.json') {
		continue;
	}
	Tool.note ("  Build function '" + fnFolder + "'");
	// copy spec
	new File (fnFolder, 'code.js').renameTo (new File (fnFolder, fnFolder.getName () + '.js'));
	new File (fnFolder, 'spec.json').renameTo (new File (fnFolder, fnFolder.getName () + '.json'));
	FileUtils.copy (new File (fnFolder, fnFolder.getName () + '.json'), new File (apiFolder, 'resources/services'), false);
	FileUtils.copy (new File (fnFolder, fnFolder.getName () + '.js'), new File (apiFolder, 'resources/scripts'), false);
	
	// update function spec
	var fnSpec = Json.load (new File (apiFolder, 'resources/services/' + fnFolder.getName () + '.json'));
	if (!fnSpec.verb) {
		fnSpec.verb = 'post';
	}
	if (!fnSpec.endpoint) {
		fnSpec.endpoint = '/' + fnFolder;
	}
	Json.store (fnSpec, new File (apiFolder, 'resources/services/' + fnFolder.getName () + '.json'));
}

Tool.command ('push api ' + apiNs);

Tool.command ('echo off');
Tool.command ('unset api.release.nocopy');
Tool.command ('echo on');


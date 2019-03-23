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
var Lang 			= native ('com.bluenimble.platform.Lang');
var Json 			= native ('com.bluenimble.platform.Json');
var File 			= native ('java.io.File');

var FileUtils 		= native ('com.bluenimble.platform.FileUtils');
var ArchiveUtils 	= native ('com.bluenimble.platform.ArchiveUtils');
var OsCommander 	= native ('com.bluenimble.platform.icli.mgm.utils.OsCommander');

/**
 *
 *	Install an api from a model file or from bnb repository
 *
 **/

// check if valid command args
if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. install [binaries url]';
}

var uuid = Lang.UUID (30);

var url = Command;

var file = new File (Home, uuid + ".commands");
var folder = new File (Home, uuid);
folder.mkdir ();

// load api spec
OsCommander.execute (
	Tool,
	Home, 
	"wget -O " + file.getAbsolutePath () + " " + url,
	null
);

ArchiveUtils.decompress (file, folder, true);

// copy scripts
var newScripts = new File (folder, 'scripts');
var allScripts = new File (Home, 'scripts');
if (newScripts.exists ()) {
	var contexts = newScripts.list ();
	if (contexts && contexts.length > 0) {
		FileUtils.copy (newScripts, allScripts, false);
		Tool.proxy ().loadScripts (allScripts);
	}
}

// copy commands
var newCommands = new File (folder, 'commands');
var allCommands = new File (Home, 'commands');
if (newCommands.exists ()) {
	var contexts = newCommands.list ();
	if (contexts && contexts.length > 0) {
		FileUtils.copy (newCommands, allCommands, false);
		Tool.proxy ().loadCommands (allCommands);
	}
}

var varsFile = new File (folder, "vars.json");
if (varsFile.exists ()) {
	var oVars = Json.load (varsFile);
	Vars.putAll (oVars);
	if (Vars ['cli.context']) {
		Tool.command ('set cli.context ' + Vars ['cli.context']);
		Tool.command ('ctx ' + Vars ['cli.context']);
	}
}

FileUtils.delete (folder);
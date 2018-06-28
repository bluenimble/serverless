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
var System 				= native ('java.lang.System');
var File 				= native ('java.io.File');
var Lang 				= native ('com.bluenimble.platform.Lang');
var FileUtils 			= native ('com.bluenimble.platform.FileUtils');
var Json 				= native ('com.bluenimble.platform.Json');
var JsonObject 			= native ('com.bluenimble.platform.json.JsonObject');
var JsonArray 			= native ('com.bluenimble.platform.json.JsonArray');
var OsCommander 		= native ('com.bluenimble.platform.icli.mgm.utils.OsCommander');

if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. client api [ApiNs required] outDirectory. Example: client api ApiNs android ~/Desktop';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 4) {
	throw 'missing command arguments. eg. client api [ApiNs required] outDirectory. Example: client api ApiNs android ~/Desktop';
}

if (tokens [0] != 'api') {
	throw 'wrong push target object. eg. client api ApiNs android ~/Desktop';
}

// api namespace
var apiNs = tokens [1];

var clientLang = tokens [2];

var outFolder = tokens [3];

Tool.command ('spec api ' + apiNs + ' ' + outFolder);

var lang = Vars['spec.lang'];
if (!lang) {
	lang = 'json';
}

if (outFolder.startsWith (Lang.TILDE + File.separator)) {
	outFolder = System.getProperty ("user.home") + outFolder.substring (1);
}
if (!outFolder.endsWith ('/')) {
	outFolder += '/'; 
}

OsCommander.execute (
	new File (Install, 'tools/oasgen'), 
	'java -jar openapi-generator-cli-3.0.3.jar generate -i ' + outFolder + apiNs + '.' + lang + 
	' -g ' + clientLang + ' -o ' + outFolder + apiNs + '-client-' + clientLang,
	null
);



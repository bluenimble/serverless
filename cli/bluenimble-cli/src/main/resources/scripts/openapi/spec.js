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
var Lang 			= native ('com.bluenimble.platform.Lang');
var FileUtils 		= native ('com.bluenimble.platform.FileUtils');
var Json 			= native ('com.bluenimble.platform.Json');
var JsonObject 		= native ('com.bluenimble.platform.json.JsonObject');
var JsonArray 		= native ('com.bluenimble.platform.json.JsonArray');


if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. spec api [ApiNs required] outDirectory. Example: spec api ApiNs ~/Desktop';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 3) {
	throw 'missing command arguments. eg. spec api [ApiNs required] outDirectory. Example: spec api ApiNs ~/Desktop';
}

if (tokens [0] != 'api') {
	throw 'wrong push target object. eg. spec api ApiNs ~/Desktop';
}

// api namespace
var apiNs = tokens [1];

var outFolder = tokens [2];
if (!outFolder.endsWith ('/')) {
	outFolder += '/';
}

var lang = Vars['spec.lang'];
if (!lang) {
	lang = 'json';
}

var noHeaders = false;
var headers = Vars['remote.headers'];
if (!headers) {
	headers = new JsonObject ();
	Vars['remote.headers'] = headers;
	noHeaders = true;
}

var currentAccept = headers.Accept;

headers.Accept = 'application/spec.openapi-3.0';

if (lang == 'json') {
	Tool.command ('desc api ' + apiNs + ' all >> file: ' + outFolder + apiNs + '.' + lang);
	Tool.info ('OpenApi Specification generated (' + outFolder + apiNs + '.' + lang + ')');
} else if (lang == 'yaml') {
	var tempVar = Lang.UUID (20);
	Tool.command ('desc api ' + apiNs + ' all >> ' + tempVar);
	Tool.command ('conv j2y ' + tempVar + ' >> file: ' + outFolder + apiNs + '.' + lang);
	Tool.info ('OpenApi Specification generated (' + outFolder + apiNs + '.' + lang + ')');
	Vars.remove (tempVar);
}

headers.Accept = currentAccept;

if (noHeaders) {
	Tool.command ('echo off');
	Vars.remove ('remote.headers');
}

Tool.command ('echo on');
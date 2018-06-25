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
var String 				= native ('java.lang.String');
var Lang 				= native ('com.bluenimble.platform.Lang');
var Json 				= native ('com.bluenimble.platform.Json');
var JsonObject 			= native ('com.bluenimble.platform.json.JsonObject');
var JsonArray 			= native ('com.bluenimble.platform.json.JsonArray');
var YamlObject			= native ('com.bluenimble.platform.cli.impls.YamlObject');

//check if valid command args
if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. conv [ConversionType] [VarToConvert required]';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 2) {
	throw 'missing command arguments. eg. conv [ConversionType] [VarToConvert required]';
}

var type 		= tokens [0];
if (type != 'j2y' && target != 'y2j') {
	throw 'invalid conversion type ' + type + '. Possible types: [j2y (from json to yaml)] and [y2j (from yaml to json)]';
}

var varToConvert = tokens [1];

var payload = Vars [varToConvert];
if (!payload) {
	throw 'variable ' + varToConvert + ' not found';
}

if (type == 'j2y') {
	// return yaml object
	Return.set (new YamlObject (payload));
}

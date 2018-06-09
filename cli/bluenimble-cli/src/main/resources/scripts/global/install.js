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

/**
 *
 *	Install an api from a model file or from bnb repository
 *
 **/
 
// check if valid command args
if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. install api [ApiNs required] [file or url of the api model spec]';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 3) {
	throw 'missing command arguments. eg. install api shopping file:///tmp/shopping.json or install api shopping https:///repo.bluenimble.com/shopping.json';
}

if (tokens [0] != 'api') {
	throw 'wrong install target object. eg. install api ....';
}

var apiNs = tokens [1];

var uri = tokens [2];

 // load api spec
Tool.command ("json load " + apiNs + " " + uri);

Tool.command ("create api " + apiNs);
	
Tool.command ("push api " + apiNs);


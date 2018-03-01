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
package com.bluenimble.platform.icli.mgm.utils;

import java.util.ArrayList;
import java.util.List;

public class DSEntity {

	private String 			pkg;
	private String 			name;
	
	private List<String> 	imports 	= new ArrayList<String> ();
	private List<String> 	annotations = new ArrayList<String> ();
	private List<DSField> 	fields 		= new ArrayList<DSField> ();
	
	public DSEntity (String name) {
		this.name = name;
	}
	
	public void addImport (String _import) {
		imports.add (_import);
	} 
	
	public List<String> getImports () {
		return imports;
	}
	
	public void addAnnotation (String annotation) {
		annotations.add (annotation);
	} 
	
	public List<String> getAnnotations () {
		return annotations;
	}
	
	public void addField (DSField field) {
		fields.add (field);
	} 
	
	public List<DSField> getFields () {
		return fields;
	}
	
	public String getName () {
		return name;
	}
	
	public String getPackage () {
		return pkg;
	}
	
	public void setPackage (String pkg) {
		this.pkg = pkg;
	}
	
}

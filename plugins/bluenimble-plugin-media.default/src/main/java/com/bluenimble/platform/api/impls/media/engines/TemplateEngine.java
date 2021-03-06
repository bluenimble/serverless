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
package com.bluenimble.platform.api.impls.media.engines;

import java.io.Serializable;
import java.io.Writer;
import java.util.Map;

import com.bluenimble.platform.api.ApiResource;

public interface TemplateEngine extends Serializable {
	
	String Templating			= "templating";	
	
	String Consumer 			= "consumer";

	String Request 				= "request";
	String Response 			= "response";

	String I18n 				= "i18n";
	
	String Output 				= "output";
	String Meta 				= "meta";
	
	String Error 				= "error";
	
	void write (ApiResource template, Map<String, Object> model, Writer writer) throws TemplateEngineException;
	
}

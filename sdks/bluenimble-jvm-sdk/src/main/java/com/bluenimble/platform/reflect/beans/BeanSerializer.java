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
package com.bluenimble.platform.reflect.beans;

import java.io.Serializable;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

public interface BeanSerializer extends Serializable {
	
	BeanSerializer 	Default 		= new DefaultBeanSerializer (0, 1);

	BeanSchema		schema	();
	
	JsonObject 		create 	(String type, int level);
	
	void			set		(String type, JsonObject json, String key, Object value);
	
	void			end 	(String type, JsonObject json);
	
}

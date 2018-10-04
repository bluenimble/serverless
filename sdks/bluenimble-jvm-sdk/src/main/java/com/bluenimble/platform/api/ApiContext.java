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
package com.bluenimble.platform.api;

import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.Referenceable;

public interface ApiContext extends Referenceable {
	
	ApiContext	Instance 	= new ApiContext () {
		private static final long serialVersionUID = 3571684809770314117L;
	
		@Override
		public void addRecyclable (String name, Recyclable r) {
		}
		@Override
		public Recyclable getRecyclable (String name) {
			return null;
		}
		@Override
		public void finish (boolean withError) {
		}
		@Override
		public void recycle () {
		}
		@Override
		public Object getReference () {
			return null;
		}
		@Override
		public void setReference (Object object) {
			
		}
	};
	
	void			addRecyclable 	(String name, Recyclable r);
	
	Recyclable		getRecyclable 	(String name);
	
	void			finish			(boolean withError);
	
	void			recycle			();

}

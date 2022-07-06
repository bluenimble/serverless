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

import java.util.concurrent.Callable;

import com.bluenimble.platform.api.impls.DefaultApiContext;

public abstract class ContextualCallable<T> implements Callable<T> {
	
	@Override
	public T call () throws Exception {
		ApiContext context = new DefaultApiContext ();
		try {
			T r = run (context);
			context.finish (false);
			return r;
		} catch (Exception ex) {
			context.finish (true);
			throw ex;
		} finally {
			context.recycle ();
		}
	}
	
	protected abstract T run (ApiContext context);

}

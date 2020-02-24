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
package com.bluenimble.platform.remote.impls.serializers;

import java.io.InputStream;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.remote.SerializationException;
import com.bluenimble.platform.remote.Serializer;

public class ByteArraySerializer implements Serializer {

	private static final long serialVersionUID = 5466668574952401976L;

	@Override
	public Object serialize (InputStream input) throws SerializationException {
		if (input == null) {
			return null;
		}
		try {
			return IOUtils.toByteArray (input);
		} catch (Exception ex) {
			throw new SerializationException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (input);
		}
	}
	
}

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
package com.bluenimble.platform.plugins.database.mongodb.tests;

import java.text.MessageFormat;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Traceable;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.plugins.database.mongodb.impls.MongoDatabaseImpl;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;

public class DatabaseServer {

	public MongoClient client () {
		CodecRegistry codecRegistry = CodecRegistries.fromRegistries (
			MongoClient.getDefaultCodecRegistry (), 
			CodecRegistries.fromProviders (PojoCodecProvider.builder ().automatic (true).build ())
		);

		MongoClientURI uri = new MongoClientURI (
			"mongodb+srv://",
			MongoClientOptions.builder ().cursorFinalizerEnabled (false).codecRegistry (codecRegistry).retryWrites (true)
		);
			
		return new MongoClient (uri);
	}
	public Database get () {
		
		return new MongoDatabaseImpl (client (), "dsdev", new Tracer () {
			private static final long serialVersionUID = 4922972723643535449L;

			@Override
			public void onInstall (Traceable traceable) {
			}
			@Override
			public void onShutdown (Traceable traceable) {
			}

			@Override
			public void log (Level level, Object o, Throwable th) {
				System.out.println (level + " > " + o + " | " + Lang.toString (th));
			}

			@Override
			public void log (Level level, Object o, Object... args) {
				System.out.println (level + " > " + MessageFormat.format (o.toString (), args));
			}
			@Override
			public boolean isEnabled (Level level) {
				return true;
			}
			
		}, false, true);
	}
	
}

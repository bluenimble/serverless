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
package com.bluenimble.platform.converter.pdf.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;
import com.convertapi.client.Config;
import com.convertapi.client.ConvertApi;
import com.convertapi.client.Param;

public class ConvertApiServiceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -6640536437255542851L;
	
	private static final String Pdf 		= "pdf";
	private static final String File 		= "file";
	private static final String FileName 	= "fileName.";
	
	private static final String Key 	= "key";
	
	interface Spec {
		String Format 	= "format";
		String InFile 	= "in";
		String OutFile 	= "out";
	}

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		Config.setDefaultSecret (Json.getString (request.getService ().getSpiDef (), Key));
		
		String format = (String)request.get (Spec.Format, ApiRequest.Scope.Parameter);
		String in = (String)request.get (Spec.InFile, ApiRequest.Scope.Parameter);
		String out = (String)request.get (Spec.OutFile, ApiRequest.Scope.Parameter);
		
		InputStream is = null;
		try {
			if (format != null) {
				is = new FileInputStream (new File (in));
				ConvertApi.convert (format, Pdf, new Param (File, is, FileName + format)).get ().saveFile (Paths.get (out)).get ();
			} else {
				ConvertApi.convertFile (Paths.get (in), Pdf).get().saveFile (Paths.get (out)).get();
			}
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (is);
		}
		
		return new JsonApiOutput (JsonObject.Blank);
	}
	
}

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

import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiSpace.Endpoint;
import com.bluenimble.platform.api.impls.JsonApiOutput;
import com.bluenimble.platform.api.impls.spis.AbstractApiServiceSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.Storage;
import com.bluenimble.platform.storage.StorageObject;

public class SplitApiServiceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = -6640536437255542851L;
	
	interface Spec {
		String File 			= "file";
		String OutputDirectory 	= "out";
		String OnFinish 		= "onFinish";
	}

	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response)
			throws ApiServiceExecutionException {
		
		String file = (String)request.getService ().getSpiDef ().get (Spec.File);
		
		JsonObject result = new JsonObject ();
		JsonArray files = new JsonArray ();
		result.set (ApiOutput.Defaults.Id, file);
		result.set (ApiOutput.Defaults.Items, files);
		
		String out 	= (String)request.getService ().getSpiDef ().get (Spec.OutputDirectory);
				
		try {
			Folder folder = feature (api, Storage.class, null, request).root ();
			
			Folder outFolder = (Folder)folder.get (out);
					
			PDDocument doc = PDDocument.load (folder.get (file).reader (request));
			PDFRenderer renderer = new PDFRenderer(doc);
			for (int i = 0; i < doc.getNumberOfPages(); i++) {
				String oid = Lang.oid ();
				StorageObject outFile = outFolder.add (null, oid, false);
				BufferedImage image = renderer.renderImageWithDPI (i, 200);
				ImageIO.write (image, "JPEG", outFile.writer (request));
				files.add (oid);
			}
		} catch (Exception ex) {
			throw new ApiServiceExecutionException (ex.getMessage (), ex);
		}
		
		if (request.getService ().getSpiDef ().containsKey (Spec.OnFinish)) {
			call (
				api, consumer, request, 
				Json.template (Json.getObject (request.getService ().getSpiDef (), Spec.OnFinish), result, true)
			);
		}
		
		return new JsonApiOutput (result);
	}
	
	public static ApiOutput call (final Api api, final ApiConsumer consumer, final ApiRequest pRequest, final JsonObject oRequest) throws ApiServiceExecutionException {
		ApiRequest request = api.space ().request (pRequest, consumer, new Endpoint () {
			@Override
			public String space () {
				return Json.getString (oRequest, ApiRequest.Fields.Space, api.space ().getNamespace ());
			}
			@Override
			public String api () {
				return Json.getString (oRequest, ApiRequest.Fields.Api, api.getNamespace ());
			}
			@Override
			public String [] resource () {
				String resource = Json.getString (oRequest, ApiRequest.Fields.Resource);
				if (resource.startsWith (Lang.SLASH)) {
					resource = resource.substring (1);
				}
				if (resource.endsWith (Lang.SLASH)) {
					resource = resource.substring (0, resource.length () - 1);
				}
				if (Lang.isNullOrEmpty (resource)) {
					return null;
				}
				return Lang.split (resource, Lang.SLASH);
			}
			@Override
			public ApiVerb verb () {
				try {
					return ApiVerb.valueOf (
						Json.getString (oRequest, ApiRequest.Fields.Verb, ApiVerb.POST.name ()).toUpperCase ()
					);
				} catch (Exception ex) {
					return ApiVerb.POST;
				}
			}
		});
		
		JsonObject parameters = Json.getObject (oRequest, ApiRequest.Fields.Data.Parameters);
		if (!Json.isNullOrEmpty (parameters)) {
			Iterator<String> keys = parameters.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				request.set (key, parameters.get (key));
			}
		}
		
		JsonObject headers = Json.getObject (oRequest, ApiRequest.Fields.Data.Headers);
		if (!Json.isNullOrEmpty (headers)) {
			Iterator<String> keys = headers.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				request.set (key, headers.get (key), Scope.Header);
			}
		}
		
		return api.call (request);
	}
	
}

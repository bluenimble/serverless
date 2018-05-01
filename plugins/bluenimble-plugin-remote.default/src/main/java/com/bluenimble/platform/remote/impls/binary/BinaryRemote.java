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
package com.bluenimble.platform.remote.impls.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.ValueHolder;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.utils.ContentTypes;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.SerializationException;
import com.bluenimble.platform.remote.Serializer;
import com.bluenimble.platform.remote.impls.AbstractRemote;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.tools.binary.BinaryClient;
import com.bluenimble.platform.tools.binary.BinaryClientCallback;
import com.bluenimble.platform.tools.binary.BinaryClientException;

public class BinaryRemote extends AbstractRemote {

	private static final long serialVersionUID = 5077033220938663135L;
	
	private static final String DefaultScheme 	= "bnbps";
	private static final String DefaultEndpoint = "bnb.binary.server";

	private BinaryClient client;
	
	public BinaryRemote (BinaryClient client) {
		this.client = client;
	}

	@Override
	public boolean post (JsonObject spec, Callback callback, ApiStreamSource... attachments) {
		return false;
	}

	@Override
	public boolean put (JsonObject spec, Callback callback, ApiStreamSource... attachments) {
		return false;
	}

	@Override
	public boolean get (JsonObject spec, Callback callback) {
		return false;
	}

	@Override
	public boolean delete (JsonObject spec, Callback callback) {
		return false;
	}

	@Override
	public boolean head (JsonObject spec, Callback callback) {
		return false;
	}

	@Override
	public boolean patch (JsonObject spec, Callback callback) {
		return false;
	}
	
	public boolean request (ApiVerb verb, JsonObject spec, Callback callback) {
		JsonObject rdata = Json.getObject (spec, Spec.Data);
		
		if (!Json.isNullOrEmpty (featureSpec)) {
			JsonObject master = featureSpec.duplicate ();
			
			Json.resolve (master, ECompiler, new VariableResolver () {
				private static final long serialVersionUID = 1L;
				@Override
				public Object resolve (String namespace, String... property) {
					Object v = Json.find (rdata, property);
					Json.remove (rdata, property);
					return v;
				}
			});
			
			spec = master.merge (spec);
		}
		
		String path = Json.getString (spec, Spec.Path);
		if (!Lang.isNullOrEmpty (path)) {
			path = Lang.SLASH;
		}
		
		Serializer.Name serName = null;
		
		try {
			serName = Serializer.Name.valueOf (Json.getString (spec, Spec.Serializer, Serializer.Name.text.name ()).toLowerCase ());
		} catch (Exception ex) {
			// ignore
			serName = Serializer.Name.text;
		}
		
		Serializer serializer = Serializers.get (serName);
		
		// contentType
		String contentType = null;

		// resole and add headers
		JsonObject headers = Json.getObject (spec, Spec.Headers);
		if (!Json.isNullOrEmpty (headers)) {
			Json.resolve (headers, ECompiler, new VariableResolver () {
				private static final long serialVersionUID = 1L;
				@Override
				public Object resolve (String namespace, String... property) {
					return Json.find (rdata, property);
				}
			});
			Iterator<String> hnames = headers.keys ();
			while (hnames.hasNext ()) {
				String hn = hnames.next ();
				String hv = Json.getString (headers, hn);
				if (HttpHeaders.CONTENT_TYPE.toUpperCase ().equals (hn.toUpperCase ())) {
					contentType = hv;
				}
			}
		}
		
		if (Lang.isNullOrEmpty (contentType)) {
			contentType = ContentTypes.FormUrlEncoded;
		}

		contentType = contentType.trim ();

		JsonObject request = new JsonObject ();
		request.set (ApiRequest.Fields.Scheme, Json.getString (featureSpec, Spec.Scheme, DefaultScheme));
		request.set (ApiRequest.Fields.Endpoint, Json.getString (featureSpec, Spec.Endpoint, DefaultEndpoint));
		request.set (ApiRequest.Fields.Verb, verb.name ());
		request.set (ApiRequest.Fields.Path, path);
		
		request.set (ApiRequest.Fields.Data.Headers, headers);
		
		JsonObject parameters = new JsonObject ();
		request.set (ApiRequest.Fields.Data.Parameters, parameters);
		
		switch (verb) {
			case GET:
				addParameters (parameters, rdata);
				break;
			case POST:
				addPayloadOrParameters (parameters, rdata, contentType);
				break;
			case DELETE:
				addParameters (parameters, rdata);
				break;	
			case PUT:
				addPayloadOrParameters (parameters, rdata, contentType);
				break;
			case PATCH:
				addPayloadOrParameters (parameters, rdata, contentType);
				break;	
			case HEAD:
				addParameters (parameters, rdata);
				break;	
			default:
				break;
		}
		
		int 	errorCodeLimit 	= Json.getInteger (spec, Spec.SuccessCode, 399);
		boolean useStreaming 	= Json.getBoolean (spec, Spec.UseStreaming, false);
		
		final ValueHolder<ByteArrayOutputStream> error = new ValueHolder<ByteArrayOutputStream> ();
		
		final ValueHolder<Integer> status = new ValueHolder<Integer> ();
		
		final ValueHolder<ByteArrayOutputStream> responseData = new ValueHolder<ByteArrayOutputStream> ();
		
		BinaryClientCallback bcc = new BinaryClientCallback () {
			@Override
			public void onStatus (int iStatus) {
				status.set (iStatus);
				if (iStatus > errorCodeLimit) {
					error.set (new ByteArrayOutputStream ());
				}
				if (useStreaming) {
					responseData.set (new ByteArrayOutputStream ());
				}
			}
			@Override
			public void onHeaders (Map<String, Object> headers) {
				
			}
			@Override
			public void onChunk (byte [] chunk) {
				if (status.get () > errorCodeLimit) {
					try {
						error.get ().write (chunk);
					} catch (IOException e) {
						throw new BinaryClientException (e.getMessage (), e);
					}
				} else if (useStreaming) {
					callback.onSuccess (
						status.get (), 
						chunk
					);
				}
			}
			@Override
			public void onFinish () {
				if (status.get () > errorCodeLimit) {
					callback.onError (status.get (), new String (error.get ().toByteArray ()));
				} else if (!useStreaming) {
					try {
						callback.onSuccess (
							status.get (), 
							serializer.serialize (new ByteArrayInputStream (responseData.get ().toByteArray ()))
						);
					} catch (SerializationException e) {
						throw new BinaryClientException (e.getMessage (), e);
					}
				}
			}
		};
		
		// send request
		client.send (request, bcc);
		
		return status.get () > errorCodeLimit;
	}
	
	private void addParameters (JsonObject parameters, JsonObject data) {
		if (Json.isNullOrEmpty (data)) {
			return;
		}
		parameters.putAll (data);
	}

	private void addPayloadOrParameters (JsonObject parameters, JsonObject data, String contentType) {
		if (Json.isNullOrEmpty (data)) {
			return;
		}
		if (ApiContentTypes.Json.equalsIgnoreCase (contentType)) {
			parameters.set (ApiRequest.Payload, data);
		} else {
			addParameters (parameters, data);
		}
	}

	@Override
	public void recycle () {
		if (client != null) {
			client.recycle ();
		}
	}

	@Override
	public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
		
	}

	@Override
	public Object get() {
		return null;
	}

}

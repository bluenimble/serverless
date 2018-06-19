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
package com.bluenimble.platform.icli.mgm.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.ArchiveUtils;
import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.parser.converters.StreamPointer;
import com.bluenimble.platform.cli.printing.impls.FriendlyJsonEmitter;
import com.bluenimble.platform.http.HttpEndpoint;
import com.bluenimble.platform.http.HttpHeader;
import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpMethods;
import com.bluenimble.platform.http.HttpParameter;
import com.bluenimble.platform.http.auth.impls.AccessSecretKeysBasedHttpRequestSigner;
import com.bluenimble.platform.http.impls.DefaultHttpClient;
import com.bluenimble.platform.http.impls.HttpHeaderImpl;
import com.bluenimble.platform.http.impls.HttpMessageBodyImpl;
import com.bluenimble.platform.http.impls.HttpParameterImpl;
import com.bluenimble.platform.http.impls.InputStreamHttpMessageBodyPart;
import com.bluenimble.platform.http.impls.StringHttpMessageBodyPart;
import com.bluenimble.platform.http.request.HttpRequest;
import com.bluenimble.platform.http.request.impls.DeleteRequest;
import com.bluenimble.platform.http.request.impls.GetRequest;
import com.bluenimble.platform.http.request.impls.HeadRequest;
import com.bluenimble.platform.http.request.impls.PatchRequest;
import com.bluenimble.platform.http.request.impls.PostRequest;
import com.bluenimble.platform.http.request.impls.PutRequest;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.http.utils.ContentTypes;
import com.bluenimble.platform.http.utils.HttpUtils;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.BlueNimble.DefaultVars;
import com.bluenimble.platform.icli.mgm.Keys;
import com.bluenimble.platform.icli.mgm.commands.mgm.RemoteCommand.Spec;
import com.bluenimble.platform.icli.mgm.remote.impls.JsonResponseReader;
import com.bluenimble.platform.icli.mgm.remote.impls.StreamResponseReader;
import com.bluenimble.platform.icli.mgm.remote.impls.TextResponseReader;
import com.bluenimble.platform.icli.mgm.remote.impls.XmlResponseReader;
import com.bluenimble.platform.icli.mgm.remote.impls.YamlResponseReader;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public class RemoteUtils {
	
	private static final DefaultExpressionCompiler Compiler = new DefaultExpressionCompiler ();

	private static final String EmptyPayload 	= "__EP__";
	
	public static final String RemoteResponseHeaders 	= "remote.response.headers";
	public static final String RemoteResponseError 	= "remote.response.error";
	
	interface ResponseActions {
		String Store 	= "store";
		String Unzip 	= "unzip";
		String Replace	= "replace";
	}
	
	private static final Map<String, Class<? extends HttpRequest>> HttpRequests = new HashMap<String, Class<? extends HttpRequest>> ();
	static {
		HttpRequests.put (HttpMethods.GET, 		GetRequest.class);
		HttpRequests.put (HttpMethods.POST, 	PostRequest.class);
		HttpRequests.put (HttpMethods.PUT, 		PutRequest.class);
		HttpRequests.put (HttpMethods.DELETE, 	DeleteRequest.class);
		HttpRequests.put (HttpMethods.HEAD, 	HeadRequest.class);
		HttpRequests.put (HttpMethods.PATCH, 	PatchRequest.class);
	}
	
	private static final Map<String, ResponseReader> Readers = new HashMap<String, ResponseReader> ();
	static {
		Readers.put (ApiContentTypes.Json, 		new JsonResponseReader ());
		Readers.put (ApiContentTypes.Yaml, 		new YamlResponseReader ());
		Readers.put (ApiContentTypes.Text, 		new TextResponseReader ());
		Readers.put (ApiContentTypes.Html, 		new TextResponseReader ());
		Readers.put (ApiContentTypes.Xml, 		new XmlResponseReader ());
		Readers.put (ApiContentTypes.Stream, 	new StreamResponseReader ());
	}
	
	private static DefaultHttpClient Http; 
	static {
		try {
			Http = new DefaultHttpClient ();
		} catch (Exception e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}
	
	public static CommandResult processRequest (Tool tool, JsonObject source, final Map<String, String> options) throws CommandExecutionException {
		
		if (Lang.isDebugMode ()) {
			tool.printer ().content ("__PS__ YELLOW:Remote Command", null);
			if (tool.printer ().isOn ()) {
				source.write (new FriendlyJsonEmitter (tool));
				tool.writeln (Lang.BLANK);
			}
		}
		
		JsonObject oKeys = null;
		
		Keys keys = BlueNimble.keys ();
		if (keys != null) {
			oKeys = keys.json ();
		} else {
			oKeys = new JsonObject ();
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		Object oTrustAll = vars.get (DefaultVars.SslTrust);
		if (oTrustAll == null) {
			Http.setTrustAll (false);
		} else {
			Http.setTrustAll (Lang.TrueValues.contains (String.valueOf (oTrustAll)));
		}
		
		List<Object> streams = new ArrayList<Object> ();
		
		HttpResponse response = null;
		
		try {
			HttpRequest request = request (oKeys, Json.getObject (source, Spec.request.class.getSimpleName ()), tool, BlueNimble.Config, options, streams);
			
			response = Http.send (request);
			
			String contentType = response.getContentType ();
			if (contentType == null) {
				contentType = ApiContentTypes.Stream;
			}
			
			int indexOfSemi = contentType.indexOf (Lang.SEMICOLON);
			
			if (indexOfSemi > 0) {
				contentType = contentType.substring (0, indexOfSemi).trim ();
			}
			
			JsonObject mediaMapping = (JsonObject)vars.get (DefaultVars.MediaMapping);
			if (mediaMapping != null) {
				contentType = Json.getString (mediaMapping, contentType, contentType);
			}
			
			ResponseReader reader = Readers.get (contentType);
			if (reader == null) {
				reader = Readers.get (ApiContentTypes.Stream);
			}
			
			List<HttpHeader> rHeaders = response.getHeaders ();
			if (rHeaders != null && !rHeaders.isEmpty ()) {
				JsonObject oHeaders = new JsonObject ();
				for (HttpHeader h : rHeaders) {
					oHeaders.set (h.getName (), Lang.join (h.getValues (), Lang.COMMA));
				}
				vars.put (RemoteResponseHeaders, oHeaders);
			}
			
			return reader.read (tool, contentType, response);
			
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		} finally {
			if (streams != null) {
				for (Object s : streams) {
					if (s instanceof InputStream) {
						IOUtils.closeQuietly ((InputStream)s);
					} else if (s instanceof StreamPointer) {
						StreamPointer sp = (StreamPointer)s;
						IOUtils.closeQuietly (sp.getStream ());
						if (sp.shouldDelete ()) {
							try {
								FileUtils.delete (sp.getPointer ());
							} catch (IOException e) {
								// IGNORE
							}
						}
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static HttpRequest request (JsonObject oKeys, JsonObject spec, Tool tool, JsonObject config, Map<String, String> options, List<Object> streams) throws Exception {
		
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		JsonObject oParameters = Json.getObject (spec, Spec.request.Parameters);

		String service = (String)eval (Json.getString (spec, Spec.request.Service), vars, config, options, oKeys, oParameters);
		
		HttpRequest request = 
				HttpRequests.get (Json.getString (spec, Spec.request.Method, HttpMethods.GET).toUpperCase ())
					.getConstructor (new Class [] { HttpEndpoint.class })
					.newInstance (
						HttpUtils.createEndpoint (new URI (service))
					);
		
		JsonObject oHeaders = Json.getObject (spec, Spec.request.Headers);

		String contentType = Json.getString (
			spec, Spec.request.ContentType, 
			Json.getString (oHeaders, ApiHeaders.ContentType)
		);

		// set headers
		List<HttpHeader> headers = request.getHeaders ();
		if (headers == null) {
			headers = new ArrayList<HttpHeader> ();
			request.setHeaders (headers);
		}

		// add default Accept header
		JsonObject defaultHeaders = (JsonObject)vars.get (DefaultVars.RemoteHeaders);
		if (!Json.isNullOrEmpty (defaultHeaders)) {
			Iterator<String> hnames = defaultHeaders.keys ();
			while (hnames.hasNext ()) {
				String hn = hnames.next ();
				if (oHeaders == null || !oHeaders.containsKey (hn)) {
					headers.add (
						new HttpHeaderImpl (
							hn, 
							defaultHeaders.getString (hn)
						)
					);
				}
			}
		}

		if (oHeaders != null && !oHeaders.isEmpty ()) {
			Iterator<String> hNames = oHeaders.keys ();
			while (hNames.hasNext ()) {
				String hName = hNames.next ();
				headers.add (
					new HttpHeaderImpl (
						hName, 
						String.valueOf (eval (Json.getString (oHeaders, hName), vars, config, options, oKeys, oParameters))
					)
				);
			}
		}
		
		
		// set parameters
		if (oParameters != null && !oParameters.isEmpty ()) {
			List<HttpParameter> parameters = request.getParameters ();
			if (parameters == null) {
				parameters = new ArrayList<HttpParameter> ();
				request.setParameters (parameters);
			}

			Iterator<String> pNames = oParameters.keys ();
			while (pNames.hasNext ()) {
				String pName = pNames.next ();
				parameters.add (
					new HttpParameterImpl (
						pName, 
						String.valueOf (eval (String.valueOf (oParameters.get (pName)), vars, config, options, oKeys, oParameters))
					)
				);
			}
		}
		
		// set body
		JsonObject oBody = Json.getObject (spec, Spec.request.Body);
		if (!Json.isNullOrEmpty (oBody)) {
			HttpMessageBody body = new HttpMessageBodyImpl ();
			request.setBody (body);

			Iterator<String> bNames = oBody.keys ();
			while (bNames.hasNext ()) {
				String bName = bNames.next ();
				
				Object bValue = oBody.get (bName);
				
				Object value = bValue instanceof String ? eval (Json.getString (oBody, bName), vars, config, options, oKeys, oParameters) : bValue;
				if (value == null) {
					continue;
				}

				if (ContentTypes.Json.equals (contentType)) {
					
					Object ov = null;
					
					if (value instanceof JsonObject) {
						ov = value;
					} else {
						String varName = String.valueOf (value);
						
						if (EmptyPayload.equals (varName)) {
							ov = JsonObject.Blank;
						} else {
							ov = ((Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS)).get (varName);
							if (ov == null) {
								throw new Exception ("variable " + value + " not found");
							}
							if (!(ov instanceof JsonObject)) {
								throw new Exception (value + " isn't a valid json object");
							}
						}
					}
					
					body.add (
						new StringHttpMessageBodyPart (ov.toString ())
					);
					
					continue;
				}
				
				String 		fileName = null;
				InputStream stream = null;
				
				if (value instanceof File) {
					File file = (File)value;
					fileName = file.getName ();
					stream = new FileInputStream (file);
					streams.add (stream);
				} else if (value instanceof StreamPointer) {
					StreamPointer sp = (StreamPointer)value;
					fileName = sp.getPointer ().getName ();
					stream = sp.getStream ();
					streams.add (sp);
				} else if (value instanceof String) {
					tool.printer ().info ("Api File/Folder ==> " + value);
					File file = new File ((String)value);
					if (!file.exists ()) {
						continue;
						//throw new Exception ("file or folder '" + value + "' doesn't exist");
					}
					fileName = file.getName ();
					if (file.isFile ()) {
						stream = new FileInputStream (file);
						streams.add (stream);
					} else if (file.isDirectory ()) {
						fileName += ".api";
						File zipFile = new File (BlueNimble.Work, Lang.UUID (20) + ".api");
						ArchiveUtils.compress (file, zipFile, true, new ArchiveUtils.CompressVisitor () {
							@Override
							public boolean onAdd (File file) {
								return !file.getName ().startsWith (Lang.DOT);
							}
						});
						
						StreamPointer sp = new StreamPointer (zipFile, true);
						streams.add (sp);
						
						stream = sp.getStream ();
					}
				}
				body.add (
					new InputStreamHttpMessageBodyPart (
						bName, fileName, 
						stream
					)
				);
			}
		}
		if (request.getBody () != null && request.getBody ().count () == 0) {
			request.setBody (null);
		}
		
		// set content type
		if (!Lang.isNullOrEmpty (contentType)) {
			if (ApiContentTypes.Multipart.equals (contentType)) {
				 if (request.getBody () != null) {
					request.setContentType (contentType);
				 }
			} else {
				request.setContentType (contentType);
			}
		}
		
		// sign request
		boolean sign = Json.getBoolean (spec, Spec.request.Sign, true);
		if (sign) {
			
			String space 		= Json.getString (oKeys, Keys.Spec.Space);
			
			String accessKey 	= Json.getString (oKeys, KeyPair.Fields.AccessKey);
			String secretKey 	= Json.getString (oKeys, KeyPair.Fields.SecretKey);
			
			AccessSecretKeysBasedHttpRequestSigner signer = 
					new AccessSecretKeysBasedHttpRequestSigner ("m>h>p>d>k>t", "Bearer", space == null ? accessKey : space + Lang.DOT + accessKey, secretKey);
			
			String timestamp = Lang.utc ();
			
			headers.add (new HttpHeaderImpl (ApiHeaders.Timestamp, timestamp));
			
			signer.getData ().put ('t', timestamp);
			
			signer.sign (request);

		}
		if (Lang.isDebugMode ()) {
			tool.printer ().content ("__PS__ YELLOW:Http Request", request.toString ());
		}

		return request;
		
	}
	
	private static Object eval (final String expression, 
			final Map<String, Object> vars, final JsonObject config, 
			final Map<String, String> options, final JsonObject keys,
			final JsonObject parameters) {
		
		if (Lang.isNullOrEmpty (expression)) {
			return expression;
		}
		
		return Compiler.compile (expression, null).eval (new VariableResolver () {
			private static final long serialVersionUID = -5398683910131117933L;
			@Override
			public Object resolve (String ns, String... aProp) {
				String defaultValue = null;
				
				String prop = Lang.join (aProp, Lang.DOT);
				
				Object value = null;
				if (ns == null) {
					value = options.get (prop);
				} else if ("cfg".equals (ns)) {
					value = config.get (prop);
				} else if ("bn".equals (ns)) {
					value = Json.find (BlueNimble.Config, Lang.split (prop, Lang.DOT));
				} else if ("vars".equals (ns)) {
					value = vars.get (prop);
				} else if ("arg".equals (ns)) {
					value = options.get (prop);
				} else if ("keys".equals (ns)) {
					value = Json.find (keys, Lang.split (prop, Lang.DOT));
				} else if ("params".equals (ns)) {
					if (parameters != null) {
						value = parameters.get (prop);
						parameters.remove (prop);
					} else {
						value = Lang.BLANK;
					} 
				}
				if (value == null) {
					return defaultValue;
				}
				return String.valueOf (value);
			}
		});
		
	}
	
}

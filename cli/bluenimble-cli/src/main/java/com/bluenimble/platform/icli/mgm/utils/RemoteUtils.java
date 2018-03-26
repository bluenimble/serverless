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
package com.bluenimble.platform.icli.mgm.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

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
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.cli.command.parser.converters.StreamPointer;
import com.bluenimble.platform.cli.impls.AbstractTool;
import com.bluenimble.platform.cli.impls.YamlObject;
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
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.json.printers.YamlStringPrinter;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public class RemoteUtils {
	
	private static final DefaultExpressionCompiler Compiler = new DefaultExpressionCompiler ();

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
	
	private static final Set<String> Printable = new HashSet<String> ();
	static {
		Printable.add (ApiContentTypes.Json);
		Printable.add (ApiContentTypes.Yaml);
		Printable.add (ApiContentTypes.Text);
		Printable.add (ApiContentTypes.Html);
		Printable.add (ApiContentTypes.Xml);
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
			tool.printer ().content ("Remote Command", source.toString ());
		}
		
		Keys keys = BlueNimble.keys ();
		if (keys == null) {
			throw new CommandExecutionException ("Security Keys not found!\nUse 'use keys yourKeys' command\nOr import a valid keys file into the the iCli\n  -> " + BlueNimble.keysFolder ().getAbsolutePath ());
		}
		
		JsonObject jSecrets = keys.json ();
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		Object oTrustAll = vars.get (DefaultVars.SslTrust);
		if (oTrustAll == null) {
			Http.setTrustAll (false);
		} else {
			Http.setTrustAll (Lang.TrueValues.contains (String.valueOf (oTrustAll)));
		}
		
		boolean isOutFile = AbstractTool.CMD_OUT_FILE.equals (vars.get (AbstractTool.CMD_OUT));
		
		List<Object> streams = new ArrayList<Object> ();
		
		HttpResponse response = null;
		
		try {
			HttpRequest request = request (keys, Json.getObject (source, Spec.request.class.getSimpleName ()), tool, BlueNimble.Config, options, streams);
			
			response = Http.send (request);
			
			String contentType = response.getContentType ();
			if (contentType == null) {
				contentType = ApiContentTypes.Text;
			}
			
			int indexOfSemi = contentType.indexOf (Lang.SEMICOLON);
			
			if (indexOfSemi > 0) {
				contentType = contentType.substring (0, indexOfSemi).trim ();
			}
			
			OutputStream out = System.out;
			
			if (Printable.contains (contentType) && !isOutFile) {
				out = new ByteArrayOutputStream ();
				response.getBody ().dump (out, "UTF-8", null);
			}
			
			if (contentType.startsWith (ApiContentTypes.Json)) {
				JsonObject result = new JsonObject (new String (((ByteArrayOutputStream)out).toByteArray ()));
				String trace = null;
				if (response.getStatus () != 200) {
					trace = result.getString ("trace");
					result.remove ("trace");
				}
				
				if (trace != null && Lang.isDebugMode ()) {
					vars.put ("Remote.Error", trace);
				}
				
				if (response.getStatus () == 200) {
					return new DefaultCommandResult (CommandResult.OK, result);
				} else {
					return new DefaultCommandResult (CommandResult.KO, result);
				}
			} else if (contentType.startsWith (ApiContentTypes.Yaml)) {
				Yaml yaml = new Yaml ();
				
				String ys = new String (((ByteArrayOutputStream)out).toByteArray ());
					   ys = Lang.replace (ys, Lang.TAB, "  ");	
					   
				@SuppressWarnings("unchecked")
				Map<String, Object> map = yaml.loadAs (ys, Map.class);
				Object trace = null;
				if (response.getStatus () != 200) {
					trace = map.get ("trace");
					map.remove ("trace");
				}
				
				if (trace != null && Lang.isDebugMode ()) {
					vars.put ("Remote.Error", trace);
				}
				
				if (response.getStatus () == 200) {
					return new DefaultCommandResult (CommandResult.OK, new YamlObject (map));
				} else {
					return new DefaultCommandResult (CommandResult.KO, new YamlObject (map));
				}
			} else if (isOutFile) {
				if (response.getStatus () == 200) {
					return new DefaultCommandResult (CommandResult.OK, response.getBody ().get (0).toInputStream ());
				} else {
					response.getBody ().dump (out, "UTF-8", null);
				}
			}
			
			JsonObject oResponse = Json.getObject (source, Spec.response.class.getSimpleName ());

			if (!Printable.contains (contentType) && response.getStatus () == 200 && oResponse != null) {
				Iterator<String> actions = oResponse.keys ();
				while (actions.hasNext ()) {
					String action = actions.next ();
					JsonObject oAction = Json.getObject (oResponse, action);
					if (action.toLowerCase ().equals (ResponseActions.Unzip)) {
						File dest = BlueNimble.Workspace;
						String folder = (String)eval (Json.getString (oAction, Spec.response.Unzip.Folder), vars, BlueNimble.Config, options, jSecrets);
						ArchiveUtils.decompress (response.getBody ().get (0).toInputStream (), new File (dest, folder));
					} else if (action.toLowerCase ().equals (ResponseActions.Replace)) {
						File file = new File (Lang.resolve (Json.getString (oAction, Spec.response.Replace.File), new Lang.VariableResolver () {
							@Override
							public String resolve (String ns, String prop) {
								if ("cfg".equals (ns)) {
									return BlueNimble.Config.getString (prop);
								}
								return String.valueOf (options.get (prop));
							}
						}));
						InputStream is = null;
						OutputStream os = null;
						try {
							is = new FileInputStream (file);
							String content = IOUtils.toString (is);
							is.close ();
							is = null;
							
							content = Lang.replace (
								content, 
								Json.getString (oAction, Spec.response.Replace.Token), 
								(String)eval (Json.getString (oAction, Spec.response.Replace.By), vars, BlueNimble.Config, options, jSecrets)
							);
							
							os = new FileOutputStream (file);
							
							IOUtils.copy (new ByteArrayInputStream (content.getBytes ()), os);
							
						} catch (IOException ex) {
							throw new CommandExecutionException (ex.getMessage (), ex);
						} finally {
							IOUtils.closeQuietly (is);
							IOUtils.closeQuietly (os);
						}
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println (e);
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
		
		if (response.getStatus () != 200) {
			return null;
		}
		
		return new DefaultCommandResult (CommandResult.OK, Json.getString (source, Spec.OkMessage));
	}
	
	private static HttpRequest request (Keys keys, JsonObject spec, Tool tool, JsonObject config, Map<String, String> options, List<Object> streams) throws Exception {
		
		JsonObject jKeys = keys.json ();
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		String space 	= keys.space 	();
		
		String accessKey = keys.accessKey ();
		String secretKey = keys.secretKey ();
		
		String service = (String)eval (Json.getString (spec, Spec.request.Service), vars, config, options, jKeys);
		
		HttpRequest request = 
				HttpRequests.get (Json.getString (spec, Spec.request.Method, HttpMethods.GET).toUpperCase ())
					.getConstructor (new Class [] { HttpEndpoint.class })
					.newInstance (
						HttpUtils.createEndpoint (new URI (service))
					);
		
		String contentType = Json.getString (spec, Spec.request.ContentType);

		// set headers
		List<HttpHeader> headers = request.getHeaders ();
		if (headers == null) {
			headers = new ArrayList<HttpHeader> ();
			request.setHeaders (headers);
		}

		JsonObject oHeaders = Json.getObject (spec, Spec.request.Headers);

		// add default Accept header application/json
		if (oHeaders == null || !oHeaders.containsKey (ApiHeaders.Accept)) {
			String accept = (String)vars.get (DefaultVars.RemoteHeadersAccept);
			if (Lang.isNullOrEmpty (accept)) {
				accept = ApiContentTypes.Json;
			}
			headers.add (
				new HttpHeaderImpl (
					ApiHeaders.Accept, 
					accept
				)
			);
		}

		if (oHeaders != null && !oHeaders.isEmpty ()) {
			Iterator<String> hNames = oHeaders.keys ();
			while (hNames.hasNext ()) {
				String hName = hNames.next ();
				headers.add (
					new HttpHeaderImpl (
						hName, 
						String.valueOf (eval (Json.getString (oHeaders, hName), vars, config, options, jKeys))
					)
				);
			}
		}
		
		
		// set parameters
		JsonObject oParameters = Json.getObject (spec, Spec.request.Parameters);
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
						String.valueOf (eval (Json.getString (oParameters, pName), vars, config, options, jKeys))
					)
				);
			}
		}
		
		// set body
		JsonObject oBody = Json.getObject (spec, Spec.request.Body);
		if (oBody != null && !oBody.isEmpty ()) {
			HttpMessageBody body = new HttpMessageBodyImpl ();
			request.setBody (body);

			Iterator<String> bNames = oBody.keys ();
			while (bNames.hasNext ()) {
				String bName = bNames.next ();
				
				Object value = eval (Json.getString (oBody, bName), vars, config, options, jKeys);
				if (value == null) {
					continue;
				}
				if (ContentTypes.Json.equals (contentType)) {
					@SuppressWarnings("unchecked")
					Object ov = ((Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS)).get (String.valueOf (value));
					if (ov == null) {
						throw new Exception ("variable " + value + " not found");
					}
					if (!(ov instanceof JsonObject)) {
						throw new Exception (value + " not valid json object");
					}
					body.add (
						new StringHttpMessageBodyPart (((JsonObject)ov).toString ())
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
								if (file.getName ().startsWith (Lang.DOT)) {
									return false;
								}
								return true;
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
		AccessSecretKeysBasedHttpRequestSigner signer = 
				new AccessSecretKeysBasedHttpRequestSigner ("m>h>p>d>k>t", "Bearer", space == null ? accessKey : space + Lang.DOT + accessKey, secretKey);
		
		String timestamp = Lang.utc ();
		
		headers.add (new HttpHeaderImpl (ApiHeaders.Timestamp, timestamp));
		
		signer.getData ().put ('t', timestamp);
		
		signer.sign (request);

		if (Lang.isDebugMode ()) {
			tool.printer ().content ("Http Request", request.toString ());
		}

		return request;
		
	}
	
	private static Object eval (final String expression, final Map<String, Object> vars, final JsonObject config, final Map<String, String> options, final JsonObject keys) {
		
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
				}
				if (value == null) {
					return defaultValue;
				}
				return String.valueOf (value);
			}
		});
		
	}
	
	@SuppressWarnings("unchecked")
	public static void main (String [] args) throws Exception {
		Yaml yaml = new Yaml ();
		
		String ys = IOUtils.toString (new FileInputStream (new File ("/tmp/yml/space.yml")));
			   
		System.out.println (new YamlStringPrinter ().print (new JsonObject (yaml.loadAs (ys, Map.class), true)).toString ());
	}
	
}

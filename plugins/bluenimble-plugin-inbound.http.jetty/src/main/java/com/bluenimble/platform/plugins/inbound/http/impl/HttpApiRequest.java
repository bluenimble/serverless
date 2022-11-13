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
package com.bluenimble.platform.plugins.inbound.http.impl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequestBodyReader;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.impls.AbstractApiRequest;
import com.bluenimble.platform.api.impls.DefaultApiStreamSource;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.iterators.EmptyIterator;
import com.bluenimble.platform.iterators.EnumerationIterator;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.inbound.http.jetty.JettyPlugin;
import com.bluenimble.platform.scripting.Scriptable;

@Scriptable (name = "ApiRequest")
public class HttpApiRequest extends AbstractApiRequest {

	private static final long serialVersionUID 					= -7827772016083090070L;
	
	private static final String 				Channel			= "http";
	
	private static final EmptyIterator<String> 	EmptyIterator	= new EmptyIterator<String> ();
	
	private static final DiskFileItemFactory 	Factory 		= new DiskFileItemFactory ();
	
	private static final Set<String>			NonPayloadTypes	= new HashSet<String> ();
	static {
		NonPayloadTypes.add (ApiContentTypes.Form);
		NonPayloadTypes.add (ApiContentTypes.Chunk);
	}
	
	private static final Set<ApiVerb>			BodyAware	= new HashSet<ApiVerb> ();
	static {
		BodyAware.add (ApiVerb.POST);
		BodyAware.add (ApiVerb.PUT);
		BodyAware.add (ApiVerb.PATCH);
	}
	
	protected 	HttpServletRequest 				proxy;
	
	private 	Map<String, ApiStreamSource> 	streams;
	private 	Map<String, Object> 			fields;
	
	public HttpApiRequest (HttpServletRequest proxy, JettyPlugin plugin) throws Exception {
		
		super ();
		
		Tracer tracer = plugin.tracer ();
		
		this.proxy = proxy;
		
		this.channel = Channel;
		
		node = new JsonObject ();
		
		// tracer.log (Tracer.Level.Info, "Process HttpRequest -> [{0}]", proxy.getRequestURI ());
		
		this.verb = ApiVerb.valueOf (proxy.getMethod ().toUpperCase ());
		
		device = new JsonObject ();
		String origin = proxy.getHeader (ApiHeaders.ClientIp);
		if (Lang.isNullOrEmpty (origin)) {
			origin = proxy.getHeader (ApiHeaders.XForwardedFor);
		}
		if (Lang.isNullOrEmpty (origin)) {
			origin = proxy.getRemoteAddr ();
		}
        device.set (Fields.Device.Origin, origin);
		Locale locale = proxy.getLocale ();
		if (locale == null) {
			locale = Locale.ENGLISH;
		}
		device.set (Fields.Device.Language, locale.getLanguage ());
		device.set (Fields.Device.Agent, proxy.getHeader (ApiHeaders.UserAgent));
		
		if (!ApiVerb.POST.equals (verb) && !ApiVerb.PUT.equals (verb)) {
			return;
		}
		try {
			if (FileUploadBase.isMultipartContent (new ServletRequestContext (proxy))) {
				ServletFileUpload upload = new ServletFileUpload (Factory);
				streams = new HashMap<String, ApiStreamSource> ();
				fields 	= new HashMap<String, Object> ();
				List<FileItem> items = upload.parseRequest (proxy);
				Iterator<FileItem> iter = items.iterator ();
				while (iter.hasNext ()) {
				    FileItem item = iter.next ();
				    tracer.log (Tracer.Level.Debug, "\t Upload Field {0}", item.getFieldName ());
				    if (item.isFormField ()) {
				    	fields.put (item.getFieldName (), item.getString ());
				    } else {
				    	streams.put (
				    		item.getFieldName (), 
				    		new DefaultApiStreamSource (
				    			item.getFieldName (), 
				    			item.getName (), 
				    			item.getContentType (), 
				    			item.getSize(), 
				    			item.getInputStream ()).setClosable (true)
				    	);
				    }
				}
				return;
			} 
			
			String contentType = proxy.getContentType ();
			
			if (contentType == null) {
				return;
			}
			
			int indexOfSemiColon = contentType.indexOf (Lang.SEMICOLON);
			if (indexOfSemiColon > 0) {
				contentType = contentType.substring (0, indexOfSemiColon);
			}
			
			if (!NonPayloadTypes.contains (contentType)) {
				ApiRequestBodyReader reader = plugin.getReader (this, contentType.toLowerCase ());
				if (reader == null) {
					reader = plugin.getReader (this, ApiContentTypes.Stream);
				}
				set (Payload, reader.read (proxy.getInputStream (), contentType, proxy.getContentLengthLong ()), Scope.Parameter);
			}
		} catch (Exception ex) {
			tracer.log (Tracer.Level.Warning, ex.getMessage (), ex);
			set (
				ApiRequest.Interceptors.Response, 
				new JsonObject ().set (
					ApiResponse.Output.Status, 
					ApiResponse.EXPECTATION_FAILED.getCode ()
				).set (
					ApiResponse.Output.Data, 
					ex.getMessage ()
				)
			);
		} 
	}

	@Override
	public String getEndpoint () {
		if (proxy.getServerPort () <= 0 || proxy.getServerPort () == 80 || proxy.getServerPort () == 443) {
			return proxy.getServerName ();
		}
		return proxy.getServerName () + Lang.COLON + proxy.getServerPort ();
	}

	@Override
	public String getScheme () {
		return proxy.getScheme ();
	}

	@Override
	public String getPath () {
		return proxy.getRequestURI ();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Iterator<String> keys (Scope scope) {
		switch (scope) {
			case Header:
				return new EnumerationIterator (proxy.getHeaderNames ());
			case Stream:
				if (streams == null || streams.isEmpty ()) {
					return EmptyIterator;
				}
				return streams.keySet ().iterator ();
			case Parameter:
				if (fields != null) {
					return fields.keySet ().iterator ();
				}
				Map params = proxy.getParameterMap ();
				if (params == null) {
					return EmptyIterator;
				}
				return params.keySet ().iterator ();
			default:
				break;
		}
		return EmptyIterator;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void forEach (Scope scope, ForEachCallback callback) {
		Iterator<String> keys = null;
		switch (scope) {
			case Header:
				Enumeration<String> headers = proxy.getHeaderNames ();
				if (headers == null) {
					break;
				}
				while (headers.hasMoreElements ()) {
					String key = headers.nextElement ();
					callback.visit (key, proxy.getHeader (key));
				}
				break;
			case Stream:
				if (streams == null || streams.isEmpty ()) {
					break;
				}
				keys = streams.keySet ().iterator ();
				while (keys.hasNext ()) {
					String key = keys.next ();
					callback.visit (key, streams.get (key));
				}
				break;
			case Parameter:
				if (application != null && !application.isEmpty ()) {
					keys = application.keySet ().iterator ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						callback.visit (key, application.get (key));
					}
				}
				
				if (fields != null && !fields.isEmpty ()) {
					keys = fields.keySet ().iterator ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						callback.visit (key, fields.get (key));
					}
				}
				
				Map params = proxy.getParameterMap ();
				if (params != null && !params.isEmpty ()) {
					keys = params.keySet ().iterator ();
					while (keys.hasNext ()) {
						String key = keys.next ();
						callback.visit (key, params.get (key));
					}
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void destroy () {
		super.destroy ();
		proxy = null;
		if (streams != null) {
			Iterator<String> sKeys = streams.keySet ().iterator ();
			while (sKeys.hasNext ()) {
				streams.get (sKeys.next ()).close ();
			}
			streams.clear ();
		}
		streams = null;
		
		if (fields != null) {
			fields.clear ();
		}
		fields = null;
	}
	
	@Override
	protected Object getByScope (String name, Scope scope) {
		switch (scope) {
			case Header:
				return proxy.getHeader (name);
			case Stream:
				if (streams == null) {
					return null;
				}
				return streams.get (name);
			case Parameter:
				Object value = null;
				if (application != null) {
					value = application.get (name);
				}
				if (value != null) {
					return value;
				}				
				value = fields == null ? null : fields.get (name);
				if (value == null) {
					if (proxy == null) {
						return null;
					}
					value = proxy.getParameter (name);
				}
				return value;
			default:
				break;
		}
		return null;
	}

	@Override
	protected void setHeader (String name, Object value) {
		throw new UnsupportedOperationException ("setHeader can't be used within " + this.getClass ().getSimpleName ());
	}
	
	public HttpServletRequest getProxy () {
		return proxy;
	}

}

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
package com.bluenimble.platform.http.request.impls;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.http.HttpEndpoint;
import com.bluenimble.platform.http.HttpHeader;
import com.bluenimble.platform.http.HttpHeaders;
import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpParameter;
import com.bluenimble.platform.http.impls.HttpHeaderImpl;
import com.bluenimble.platform.http.request.HttpRequestWriteException;
import com.bluenimble.platform.http.utils.ContentTypes;
import com.bluenimble.platform.http.utils.HttpUtils;

public class BodyAwareRequest extends AbstractHttpRequest {

	private static final long serialVersionUID = -3367718889542333065L;
	
	protected BodyAwareRequest (String name, HttpEndpoint endpoint) {
		super (name, endpoint);
		contentType = ContentTypes.FormUrlEncoded;
		if (!Lang.isNullOrEmpty (endpoint.getQuery ())) {
			parameters = new ArrayList<HttpParameter> ();
			HttpUtils.parseParameters (endpoint.getQuery (), parameters);
			setParameters (parameters);
			endpoint.setQuery (null);
		}
	}

	@Override
	public void write (HttpURLConnection hc) throws HttpRequestWriteException {
		
		hc.setUseCaches (isCachingEnabled ());

		String boundary = setContentType ();
		
		if (visitor != null) {
			visitor.visit (this, hc);
		}
		
		addHeaders (hc);
		
		Writer writer = null;
		try {
			
			OutputStream os = null;
			
			if (isDebugMode ()) {
				os = System.out;
			} else {
				os = hc.getOutputStream ();
			}

			OutputStreamWriter osw = 
					(charset == null ? new OutputStreamWriter (os) : 
									   new OutputStreamWriter (os, charset));
			writer = new PrintWriter (osw, true);
			
			if (hasParameters () && getBodyPartsCount () == 0) {
				writer.append (super.dumpParameters ()).flush ();
				return;
			}
			if (getBodyPartsCount () > 0) {
				if (!doesntNeedBoundary ()) {
					String cs = charset != null ? charset : HttpUtils.DEFAULT_ENCODING;
					for (HttpParameter p : getParameters ()) {
						writer.append ("--" + boundary).append (HttpMessageBody.CRLF);
					    writer.append ("Content-Disposition: form-data; name=\"" + p.getName () + "\"").append (HttpMessageBody.CRLF);
					    writer.append ("Content-Type: text/plain; charset=" + cs).append (HttpMessageBody.CRLF);
					    writer.append (HttpMessageBody.CRLF);
					    writer.append (String.valueOf (p.getValue ())).append (HttpMessageBody.CRLF).flush ();
					}
				} 
				body.dump (os, charset, boundary);
				return;
			}
			
		} catch (Throwable th) {
			throw new HttpRequestWriteException (th);
		} finally {
			if (writer != null) {
				try {
					writer.close ();
				} catch (IOException e) {
					// IGNORE
				}
			}
		} 

	}
	
	private String setContentType () {
		String boundary = getBoundary ();

		String ct = contentType;
		
		if (boundary != null) {
			ct = contentType + "; boundary=" + boundary;
		} 
		List<HttpHeader> headers = getHeaders ();
		if (headers == null) {
			headers = new ArrayList<HttpHeader> ();
			setHeaders (headers);
		}
		headers.add (new HttpHeaderImpl (HttpHeaders.CONTENT_TYPE, ct));
		return boundary;
	}

	/**
	 * return boundary 	if hasParameters and at least body
	 * 					if more than one body
	 * @return
	 */
	private String getBoundary () {
		
		// no body and no parameters
		if (doesntNeedBoundary ()) {
			return null;
		}
		
		// else, create a boundary
		return Long.toHexString (System.currentTimeMillis ());
	}
	
	private boolean doesntNeedBoundary () {
		// no body and no parameters
		// one body and contentType isnt multipart nor form-data
		return 	(
					getBodyPartsCount () == 0 && 
					!hasParameters ()
				) ||
				(
					getBodyPartsCount () == 1 && 
					!ContentTypes.Multipart.equals (contentType) && 
					!ContentTypes.FormUrlEncoded.equals (contentType)
				);
	}
	
	private int getBodyPartsCount () {
		if (body == null) {
			return 0;
		}
		return body.count ();
	}
	
	@Override
	protected String dumpParameters () throws UnsupportedEncodingException {
		if (doesntNeedBoundary ()) {
			return super.dumpParameters ();
		}
		return null;
	}
	
	@Override
	public URI getURI () throws UnsupportedEncodingException, URISyntaxException {
		return HttpUtils.createURI (endpoint, dumpParameters ());
	}

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder ();
		try {
			sb.append (name).append (" ").append (getURI ()).append ("\n");
			if (parameters != null && !parameters.isEmpty ()) {
				sb.append ("<PARAMS>\n");
				for (HttpParameter p : parameters) {
					sb.append ("\t").append ("[").append (p.getName ()).append ("]=>").append (p.getValue ()).append ("\n");
				}
			} else {
				sb.append ("<NO PARAMS>");
			}
		} catch (Exception e) {
		}
		
		sb.append (super.toString ());
		
		if (getBodyPartsCount () > 0) {
			sb.append ("\n").append ("<BODY PARTS>").append (getBodyPartsCount ());
		}
		
		String s = sb.toString ();
		sb.setLength (0);
		sb = null;
		return s;
	}

}

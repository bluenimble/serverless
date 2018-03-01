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
package com.bluenimble.platform.http.impls;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpMessageBodyPart;

public class HttpMessageBodyImpl implements HttpMessageBody {
	
	private static final long serialVersionUID = -4689114412779498380L;

	protected List<HttpMessageBodyPart> parts;
	
	public HttpMessageBodyImpl () {
	}
	public HttpMessageBodyImpl (HttpMessageBodyPart content) {
		add (content);
	}
	
	@Override
	public void dump (OutputStream output, String charset, String boundary) throws IOException {
		
		OutputStreamWriter osw = null;
		
		if (charset == null) {
			osw = new OutputStreamWriter (output);
		} else {
			osw = new OutputStreamWriter (output, charset);
		}
		
		Writer writer = new PrintWriter (osw, true);
		
		for (int i = 0; i < count (); i++) {
			
			HttpMessageBodyPart part = parts.get (i);
			
			if (boundary != null) {
				writer.append ("--" + boundary).append (CRLF);
				
				String name = part.getName ();
				if (name == null) {
					name = "Part-" + i;
				}
				
				String fname = part.getFileName ();
				if (fname == null) {
					fname = name;
				}
				
			    writer.append ("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fname + "\"").append (CRLF);
			    if (part.getContentType () != null) {
			    	writer.append ("Content-Type: " + part.getContentType ()).append (CRLF);
				}
			    if (part.getTransferEncoding () != null) {
			    	writer.append ("Content-Transfer-Encoding: " + part.getTransferEncoding ()).append (CRLF);
				}
			    
			    writer.append (CRLF).flush ();
			}
			
			part.dump (output, charset);
			
			if (boundary != null) {
				writer.append (CRLF).flush (); 
			}
			
		}
		if (boundary != null) {
		    writer.append ("--" + boundary + "--").append (HttpMessageBody.CRLF).flush ();
		}
	}

	@Override
	public void add (HttpMessageBodyPart part) {
		if (parts == null) {
			parts = new ArrayList<HttpMessageBodyPart> ();
		}
		parts.add (part);
	}
	
	@Override
	public HttpMessageBodyPart get (int index) {
		if (parts == null) {
			return null;
		}
		return parts.get (index);
	}
	@Override
	public int count () {
		if (parts == null) {
			return 0;
		}
		return parts.size ();
	}
	
}

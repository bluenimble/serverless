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
package com.bluenimble.platform.api.impls.media;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Lang.VariableResolver;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiMediaException;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.media.ApiMediaProcessor;
import com.bluenimble.platform.api.media.DataWriter;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.http.utils.HttpUtils;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.plugins.media.utils.DefaultVariableResolver;
import com.bluenimble.platform.server.plugins.media.utils.MediaRoutingUtils;
import com.bluenimble.platform.server.plugins.media.utils.WriteResponseUtils;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

public class StreamMediaProcessor implements ApiMediaProcessor {

	private static final long serialVersionUID = -3490327410756493328L;
	
	public static final String Name = "stream";
	
    private static final long 		EXPIRE_TIME 			= 604800000L; // ..ms = 1 week.
    private static final int 		BUFFER_SIZE 			= 32 * 1024;

	private static 	final String 	HeadersDateFormat 		= "EEE, dd MMM yyyy HH:mm:ss zzz";
	
	private static final String 	RangeBytes 				= "bytes";
	private static final String 	MultipartBoundary 		= "multipart/byteranges; boundary=";

	private static final String 	Resource = "resource";

	@Override
	public void process (Api api, ApiService service, ApiConsumer consumer, ApiOutput output, ApiRequest request, ApiResponse response) 
			throws ApiMediaException {
		
		String contentType = output != null ? output.contentType () : null;
		if (Lang.isNullOrEmpty (contentType)) {
			contentType = (String)request.get (ApiRequest.SelectedMedia); 
		}
		if (Lang.isNullOrEmpty (contentType) || ApiMediaProcessor.Any.equals  (contentType)) {
			contentType = ApiContentTypes.Stream;
		}
		
		// if error
		if (WriteResponseUtils.writeError (response, api.tracer (), contentType)) {
			try {
				response.close ();
			} catch (Exception e) {
				throw new ApiMediaException (e.getMessage (), e);
			}
			return;
		}
		
		// if no output
		if (output == null) {
			response.setStatus (ApiResponse.NOT_FOUND);
			response.flushHeaders ();
			try {
				response.close ();
			} catch (Exception e) {
				throw new ApiMediaException (e.getMessage (), e);
			}
			return;
		} 
		
		// process cache
		if (processCache (request, response, output)) {
			response.flushHeaders ();
			try {
				response.close ();
			} catch (Exception e) {
				throw new ApiMediaException (e.getMessage (), e);
			}
			return;
		}
		
		JsonObject 	mediaDef = MediaRoutingUtils.pickMedia (api, service, contentType);
		
		response.set (ApiHeaders.ContentType, contentType);
		
		VariableResolver vr = new DefaultVariableResolver (request, output != null ? output.data () : null, output != null ? output.meta () : null);
		
		JsonObject media = MediaRoutingUtils.processMedia (
			request, response, 
			vr, 
			mediaDef, api.tracer ()
		);
		
		ApiResource resource = null;
		
		if (media != null) {
			// if there is an associated resource
			String sResource = Json.getString (media, Resource);
			
			if (!Lang.isNullOrEmpty (sResource)) {
				try {
					resource = api.getResourcesManager ().get (Lang.split (Lang.resolve (sResource, vr), Lang.SLASH));
				} catch (ApiResourcesManagerException ex) {
					throw new ApiMediaException (ex.getMessage (), ex);
				} 
			}
		}
		
		OutputStream ros = null;
		
		try {
			
			ros = response.toOutput ();
			
			// if a resource attached
			if (resource != null) {
				response.flushHeaders ();
				resource.pipe (ros, 0, -1);
				return;
			} 
			
			// write content or ranges if requested
			writeRanges (request, response, ros, output, contentType);
			
		} catch (Exception ex) {
			throw new ApiMediaException (ex.getMessage (), ex);
		} finally {
			try {
				response.close ();
			} catch (IOException e) {
			}
		}
		
	}
	
	private boolean processCache (ApiRequest request, ApiResponse response, ApiOutput output) {
        // Validate request headers for caching ---------------------------------------------------

		String id 		= (String)output.get (ApiOutput.Defaults.Id);
		Date timestamp 	= (Date)output.get (ApiOutput.Defaults.Timestamp);
		
        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = (String)request.get (ApiHeaders.IfNoneMatch, Scope.Header);
        if (id != null && ifNoneMatch != null && HttpUtils.matches (ifNoneMatch, id)) {
            response.set (ApiHeaders.IfNoneMatch, id); // Required in 304.
            response.setStatus (ApiResponse.NOT_MODIFIED);
            return true;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        Date dIfModifiedSince = date (request, ApiHeaders.IfModifiedSince);
        
		if (timestamp != null && dIfModifiedSince != null && !timestamp.after (dIfModifiedSince)) {
            response.set (ApiHeaders.ETag, id); // Required in 304.
            response.setStatus (ApiResponse.NOT_MODIFIED);
            return true;
		}

        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = (String)request.get (ApiHeaders.IfMatch, Scope.Header); 
        if (id != null && ifMatch != null && !HttpUtils.matches (ifMatch, id)) {
        	response.setStatus (ApiResponse.PRECONDITION_FAILED);
            return true;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        Date dIfUnmodifiedSince = date (request, ApiHeaders.IfUnmodifiedSince);
        if (timestamp != null && dIfUnmodifiedSince != null && timestamp.after (dIfUnmodifiedSince)) {
            response.setStatus (ApiResponse.PRECONDITION_FAILED);
            return true;
        }
        return false;
	}
	
	private void writeRanges (ApiRequest request, ApiResponse response, final OutputStream ros, ApiOutput output, String contentType) throws IOException {
		
		String id 			= (String)output.get (ApiOutput.Defaults.Id);
		Date timestamp 		= (Date)output.get (ApiOutput.Defaults.Timestamp);
		final long length 		= output.length ();
		
        // Prepare some variables. The full Range represents the complete file.
		Chunk full = new Chunk (0, output.length () - 1);
        List<Chunk> chunks = new ArrayList <Chunk>();

        // Validate and process Range and If-Range headers.
        String range = (String)request.get (ApiHeaders.Range, Scope.Header);
        
        if (!Lang.isNullOrEmpty (range)) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.set (ApiHeaders.ContentRange, "bytes */" + output.length ()); // Required in 416.
                response.setStatus (ApiResponse.REQUESTED_RANGE_NOT_SATISFIABLE);
				response.flushHeaders ();
                return;
            }

            String ifRange = (String)request.get (ApiHeaders.IfRange, Scope.Header);
            if (ifRange != null && !ifRange.equals (id)) {
                try {
                    Lang.toDate (ifRange, HeadersDateFormat);  // Throws IAE if invalid.
                } catch (ParseException ignore) {
                	chunks.add (full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (chunks.isEmpty ()) {
                for (String part : range.substring (6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = sublong (part, 0, part.indexOf ("-"));
                    long end = sublong (part, part.indexOf ("-") + 1, part.length ());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        response.set (ApiHeaders.ContentRange, "bytes */" + length); // Required in 416.
        				response.flushHeaders ();
                        response.setStatus (ApiResponse.REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Add range.                    
                    chunks.add (new Chunk (start, end));
                }
            }
        }

        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set content disposition.
        String disposition = (String)output.get (ApiOutput.Defaults.Disposition);
        if (Lang.isNullOrEmpty (disposition)) {
        	disposition = ApiOutput.Disposition.Inline;
        }

        // Initialize response.
        response.setBuffer (BUFFER_SIZE);
        response.set (ApiHeaders.ContentType, contentType);
        response.set (ApiHeaders.ContentDisposition, disposition + ";filename=\"" + output.name () + "\"");
        response.set (ApiHeaders.AcceptRanges, RangeBytes);
        if (id != null) {
            response.set (ApiHeaders.ETag, id);
        }
        if (timestamp != null) {
            response.set (ApiHeaders.LastModified, Lang.toString (timestamp, HeadersDateFormat));
        }
        
        Long expireTime = (Long)output.get (ApiOutput.Defaults.Expires);
        if (expireTime == null) {
        	expireTime = EXPIRE_TIME;
        }
        
        response.set (ApiHeaders.Expires, Lang.toString (new Date (System.currentTimeMillis () + expireTime), HeadersDateFormat) );
        
        // Send requested file (part(s)) to client ------------------------------------------------
        // Return full file.
        if (chunks.isEmpty () || chunks.get (0) == full) {
            response.set (ApiHeaders.ContentType , contentType);
            response.set (ApiHeaders.ContentRange, RangeBytes + Lang.SPACE + full.start () + Lang.DASH + full.end () + Lang.SLASH + length);
            response.set (ApiHeaders.ContentLength, full.size ());
            
            response.flushHeaders ();
            
            output.pipe (ros, full.start (), full.size ());
            
        } else if (chunks.size () == 1) {
            Chunk chunk = chunks.get (0);
            response.set (ApiHeaders.ContentType , contentType);
            response.set (ApiHeaders.ContentRange, RangeBytes + Lang.SPACE + chunk.start () + Lang.DASH + chunk.end () + Lang.SLASH + length);
            response.set (ApiHeaders.ContentLength, chunk.size ());
            response.setStatus (ApiResponse.PARTIAL_CONTENT); // 206.

            response.flushHeaders ();
            
            output.pipe (ros, chunk.start (), chunk.size ());

        } else {
        	
        	final String boundry = Long.toHexString (System.currentTimeMillis ());

            // Return multiple parts of file.
            response.set (ApiHeaders.ContentType, MultipartBoundary + boundry);
            response.setStatus (ApiResponse.PARTIAL_CONTENT); // 206.
            
            response.flushHeaders ();
            
            final String ct = contentType;
            
            output.pipe (ros, new StreamDecorator () {
				@Override
				public void start () throws IOException {}
				
				@Override
				public void pre (Chunk chunk, int index) throws IOException {
					ros.write (Lang.ENDLN.getBytes ());
                    ros.write (("--" + boundry + Lang.ENDLN).getBytes ());
                    ros.write ((ApiHeaders.ContentType + Lang.COLON + Lang.SPACE + ct + Lang.ENDLN).getBytes ());
                    ros.write (
                    	(
                    		ApiHeaders.ContentRange + Lang.COLON + Lang.SPACE + RangeBytes + Lang.SPACE + 
                    		chunk.start () + Lang.DASH + chunk.end () + Lang.SLASH + length + Lang.ENDLN
                    	).getBytes ()
                    );
				}
				
				@Override
				public void post (Chunk chunk, int index) throws IOException {}
				
				@Override
				public void end () throws IOException {
					ros.write (("\n--" + boundry + "--\n").getBytes ());
				}
			}, (Chunk [])chunks.toArray ());
        }
    }

	@Override
	public void addWriter (String name, DataWriter writer) {
	}
	
	@Override
	public void removeWriter (String name) {
	}

	private Date date (ApiRequest request, String header) {
        String sDate = (String)request.get (header, Scope.Header); 
        
        Date date = null;
        if (!Lang.isNullOrEmpty (sDate)) {
        	try {
				date = Lang.toDate (sDate, HeadersDateFormat);
			} catch (ParseException e) {
				return null;
			}
        }
        return date;
	}
	
	private static long sublong (String value, int beginIndex, int endIndex) {
        String substring = value.substring (beginIndex, endIndex);
        return (substring.length () > 0) ? Long.parseLong (substring) : -1;
    }
    
}

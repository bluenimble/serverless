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
package com.bluenimble.platform.http.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.http.HttpEndpoint;
import com.bluenimble.platform.http.HttpParameter;
import com.bluenimble.platform.http.impls.HttpParameterImpl;

public class HttpUtils {
	
	public static final String DEFAULT_ENCODING = "UTF-8";
	
    public static String urlEncode (String value, boolean path) {
        try {
            String encoded = URLEncoder.encode (value, DEFAULT_ENCODING)
                    .replace("+", "%20").replace("*", "%2A")
                    .replace("%7E", "~");
            if (path) {
                encoded = encoded.replace("%2F", "/");
            }

            return encoded;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean isUsingNonDefaultPort(URI uri) {
        String scheme = uri.getScheme().toLowerCase();
        int port = uri.getPort();

        if (port <= 0) return false;
        if (scheme.equals("http") && port == 80) return false;
        if (scheme.equals("https") && port == 443) return false;

        return true;
    }
	
    public static URI createURI (HttpEndpoint endpoint, String params) throws URISyntaxException {
		return createURI (endpoint.getScheme (), endpoint.getHost (),
				endpoint.getPort (), endpoint.getPath (), params, null);
	}
	
	public static URI createURI (String scheme, String host,
			int port, String path, String query,
			String fragment) throws URISyntaxException {

		StringBuilder buffer = new StringBuilder();
		if (host != null) {
			if (scheme != null) {
				buffer.append (scheme);
				buffer.append ("://");
			}
			buffer.append(host);
			if (port > 0) {
				buffer.append (':');
				buffer.append (port);
			}
		}
		
		buffer.append (path);
		
		if (query != null) {
			buffer.append (Lang.QUESTION);
			buffer.append (query);
		}
		if (fragment != null) {
			buffer.append ('#');
			buffer.append (fragment);
		}
		
		String uri = buffer.toString ();
		buffer.setLength (0);
		
		return new URI (uri);
	}
	
	public static String encodePath (String path) {
		String p = Lang.BLANK;
		String [] resources = Lang.split (path, Lang.SLASH);
		if (resources != null && resources.length > 0) {
			for (String r : resources) {
				if (!Lang.isNullOrEmpty (r)) {
					p += Lang.SLASH + urlEncode (r, false);
				}
			}
		}
		if (Lang.isNullOrEmpty (p)) {
			p = Lang.SLASH;
		}
		if (!p.startsWith (Lang.SLASH)) {
			p = Lang.SLASH + p;
		}
		return p;
	}

	public static URI rewriteURI (final URI uri, final HttpEndpoint target,
			boolean dropFragment) throws URISyntaxException {
		if (uri == null) {
			throw new IllegalArgumentException("URI may nor be null");
		}
		if (target != null) {
			return createURI (target.getScheme (), target
					.getHost (), target.getPort(), normalizePath(uri
					.getRawPath()), uri.getRawQuery(), dropFragment ? null
					: uri.getRawFragment());
		} else {
			return createURI(null, null, -1, normalizePath(uri
					.getRawPath()), uri.getRawQuery(), dropFragment ? null
					: uri.getRawFragment());
		}
	}

	private static String normalizePath (String path) {
		if (path == null) {
			return null;
		}
		int n = 0;
		for (; n < path.length(); n++) {
			if (path.charAt(n) != '/') {
				break;
			}
		}
		if (n > 1) {
			path = path.substring(n - 1);
		}
		return path;
	}

	public static URI rewriteURI (final URI uri, final HttpEndpoint target)
			throws URISyntaxException {
		return rewriteURI (uri, target, false);
	}

	public static URI resolve (final URI baseURI, final String reference) {
		return resolve(baseURI, URI.create(reference));
	}

	public static URI resolve (final URI baseURI, URI reference) {
		if (baseURI == null) {
			throw new IllegalArgumentException("Base URI may nor be null");
		}
		if (reference == null) {
			throw new IllegalArgumentException("Reference URI may nor be null");
		}
		String s = reference.toString();
		if (s.startsWith("?")) {
			return resolveReferenceStartingWithQueryString(baseURI, reference);
		}
		boolean emptyReference = s.length() == 0;
		if (emptyReference) {
			reference = URI.create("#");
		}
		URI resolved = baseURI.resolve(reference);
		if (emptyReference) {
			String resolvedString = resolved.toString();
			resolved = URI.create(resolvedString.substring(0, resolvedString
					.indexOf('#')));
		}
		return removeDotSegments(resolved);
	}

	private static URI resolveReferenceStartingWithQueryString(
			final URI baseURI, final URI reference) {
		String baseUri = baseURI.toString();
		baseUri = baseUri.indexOf('?') > -1 ? baseUri.substring(0, baseUri
				.indexOf('?')) : baseUri;
		return URI.create(baseUri + reference.toString());
	}

	private static URI removeDotSegments (URI uri) {
		String path = uri.getPath();
		if ((path == null) || (path.indexOf("/.") == -1)) {
			// No dot segments to remove
			return uri;
		}
		String[] inputSegments = path.split("/");
		Stack<String> outputSegments = new Stack<String>();
		for (int i = 0; i < inputSegments.length; i++) {
			if ((inputSegments[i].length() == 0)
					|| (".".equals(inputSegments[i]))) {
				// Do nothing
			} else if ("..".equals(inputSegments[i])) {
				if (!outputSegments.isEmpty()) {
					outputSegments.pop();
				}
			} else {
				outputSegments.push(inputSegments[i]);
			}
		}
		StringBuilder outputBuffer = new StringBuilder();
		for (String outputSegment : outputSegments) {
			outputBuffer.append('/').append(outputSegment);
		}
		try {
			return new URI(uri.getScheme(), uri.getAuthority(), outputBuffer
					.toString(), uri.getQuery(), uri.getFragment());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static HttpEndpoint createEndpoint (final URI uri) {
		if (uri == null) {
			return null;
		}
		HttpEndpoint target = null;
		if (uri.isAbsolute ()) {
			int port = uri.getPort (); // may be overridden later
			String host = uri.getHost ();
			if (host == null) { // normal parse failed; 
				// authority does not seem to care about the valid charset
				// for host names
				host = uri.getAuthority ();
				if (host != null) {
					// Strip off any leading user credentials
					int at = host.indexOf('@');
					if (at >= 0) {
						if (host.length() > at + 1) {
							host = host.substring(at + 1);
						} else {
							host = null; // @ on its own
						}
					}
					// Extract the port suffix, if present
					if (host != null) {
						int colon = host.indexOf(':');
						if (colon >= 0) {
							if (colon + 1 < host.length()) {
								port = Integer.parseInt(host
										.substring(colon + 1));
							}
							host = host.substring(0, colon);
						}
					}
				}
			}
			if (host != null) {
				target = new HttpEndpoint (uri.getScheme (), host, port, uri.getPath (), uri.getQuery ());
			}
		}
		return target;
	}

	public static void parseParameters(String query, List<HttpParameter> parameters) {
		if (Lang.isNullOrEmpty (query)) {
			return;
		}
		
		String [] aParams = Lang.split (query, Lang.AMP);
		for (String pv : aParams) {
			int indexOfEquals = pv.indexOf (Lang.EQUALS);
			String name = null;
			String value = null;
			if (indexOfEquals > 0) {
				name = pv.substring (0, indexOfEquals);
				value = pv.substring (indexOfEquals + 1);
			} else {
				name = pv;
			}
			parameters.add (new HttpParameterImpl (name, value));
		}
		
	}
	
	/**
     * Returns true if the given accept header accepts the given value.
     * @param acceptHeader The accept header.
     * @param toAccept The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    public static boolean accepts (String acceptHeader, String toAccept) {
        String [] acceptValues = acceptHeader.split ("\\s*(,|;)\\s*");
        Arrays.sort (acceptValues);

        return Arrays.binarySearch (acceptValues, toAccept) > -1
                || Arrays.binarySearch (acceptValues, toAccept.replaceAll ("/.*$", "/*")) > -1
                || Arrays.binarySearch (acceptValues, "*/*") > -1;
    }

    /**
     * Returns true if the given match header matches the given value.
     * @param matchHeader The match header.
     * @param toMatch The value to be matched.
     * @return True if the given match header matches the given value.
     */
    public static boolean matches (String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
                || Arrays.binarySearch(matchValues, "*") > -1;
    }

}

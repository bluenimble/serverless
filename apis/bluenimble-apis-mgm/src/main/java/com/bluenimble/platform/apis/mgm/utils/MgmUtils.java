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
package com.bluenimble.platform.apis.mgm.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiAccessDeniedException;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.DescribeOption;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonObject;

public class MgmUtils {

	public static final String TemplatesFolder 	= "api-templates";
	public static final String TemplatesFile 	= "templates.json";
	
	private static final String VerbosePostfix = "-v";
	
	private static final Map<String, DescribeOption.Option> DescribeOptions = new HashMap<String, DescribeOption.Option> ();
	static {
		for (DescribeOption.Option o : DescribeOption.Option.values ()) {
			DescribeOptions.put (o.name ().toLowerCase (), o);
		}
	}

	public static ApiSpace space (ApiConsumer consumer, Api api) throws ApiAccessDeniedException {
		String spaceNs = (String)consumer.get (ApiConsumer.Fields.Space);
		if (Lang.isNullOrEmpty (spaceNs)) {
			throw new ApiAccessDeniedException ("space not found in key pair");
		}
		return api.space ().space (spaceNs);
	}
	
	public static Api api (ApiConsumer consumer, Api api, String apiNs) throws ApiAccessDeniedException {
		return space (consumer, api).api (apiNs);
	}
	
	public static DescribeOption [] options (String spec, DescribeOption _default) {
		
		if (Lang.isNullOrEmpty (spec)) {
			if (_default == null) {
				return null;
			}
			return new DescribeOption [] { _default};
		}

		Set<DescribeOption> options = new HashSet<DescribeOption> ();
		
		String [] aOptions = Lang.split (spec, Lang.COMMA, true);
		for (String o : aOptions) {
			
			boolean verbose = false;
			
			o = o.toLowerCase ();
			
			if (o.endsWith (VerbosePostfix)) {
				o = o.substring (0, o.length () - VerbosePostfix.length ());
				verbose = true;
			}
			
			String optName 			= o;
			String offsetAndLength 	= null;
			
			int indexOfColon = o.indexOf (Lang.COLON);
			if (indexOfColon > 0) {
				optName 		= o.substring (0, indexOfColon).trim ();
				offsetAndLength	= o.substring (indexOfColon + 1).trim ();
			}
			
			DescribeOption.Option option = DescribeOptions.get (optName);
			if (option == null) {
				continue;
			}
			
			int offset = 0;
			int length = -1;
			
			if (!Lang.isNullOrEmpty (offsetAndLength)) {
				int indexOfDash = o.indexOf (Lang.DASH);
				if (indexOfDash > 0) {
					try {
						offset 		= Integer.valueOf (offsetAndLength.substring (0, indexOfDash).trim ());
					} catch (NumberFormatException nfex) {
						// IGNORE, default to 0
					}
					try {
						length 		= Integer.valueOf (offsetAndLength.substring (indexOfDash + 1).trim ());
					} catch (NumberFormatException nfex) {
						// IGNORE, default to -1
					}
				} else {
					try {
						length 		= Integer.valueOf (offsetAndLength);
					} catch (NumberFormatException nfex) {
						// IGNORE, default to -1
					}
				}
			}
			
			options.add (new DescribeOption (option, verbose, offset, length));
			
		}
		
		DescribeOption [] array = new DescribeOption [options.size ()];
		
		options.toArray (array);
		
		return array;
		
	}
	
	public static boolean isSecure (ApiService service) {
		JsonObject security = service.getSecurity ();
        return 	security == null || 
        		!security.containsKey (ApiService.Spec.Security.Enabled) || 
        		security.get (ApiService.Spec.Security.Enabled) == "true";
	}
	
}

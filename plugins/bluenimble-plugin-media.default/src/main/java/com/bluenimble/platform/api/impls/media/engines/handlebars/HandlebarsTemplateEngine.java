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
package com.bluenimble.platform.api.impls.media.engines.handlebars;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngine;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngineException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.plugins.media.MediaPlugin;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.TemplateSource;

public class HandlebarsTemplateEngine implements TemplateEngine {

	private static final long serialVersionUID = -3029837568501446988L;
	
	private static final String StartDelimitter 		= "startDelimitter";
	private static final String EndDelimitter 			= "endDelimitter";
	
	private static final String DefaultStartDelimitter 	= "[[";
	private static final String DefaultEndDelimitter 	= "]]";
	
	protected Map<String, CachedTemplate> templates = new ConcurrentHashMap<String, CachedTemplate> ();
	
	private Handlebars engine;
	private JsonObject config;
	
	public HandlebarsTemplateEngine (MediaPlugin plugin, Api api) {
		config = (JsonObject)Json.find (api.getFeatures (), plugin.getNamespace (), MediaPlugin.HandlebarsEngine);
		engine = new Handlebars (new HandlebarsTemplateLoader (plugin, api));
		engine.startDelimiter (Json.getString (config, StartDelimitter, DefaultStartDelimitter));
		engine.endDelimiter (Json.getString (config, EndDelimitter, DefaultEndDelimitter));
		
		engine.registerHelper ("json", new Helper<JsonObject>() {
			public CharSequence apply (JsonObject data, Options options) {
				return new Handlebars.SafeString (data.toString ());
			}
		});
		engine.registerHelper ("truncate", new Helper<String>() {
			public CharSequence apply (String data, Options options) {
				Integer from 	= options.param (0, 0);
				Integer to 		= options.param (1, data.length ());
				return new Handlebars.SafeString (data.substring (from, to));
			}
		});
		engine.registerHelper ("uppercase", new Helper<String>() {
			public CharSequence apply (String data, Options options) {
				return new Handlebars.SafeString (data.toUpperCase ());
			}
		});
		engine.registerHelper ("lowercase", new Helper<String>() {
			public CharSequence apply (String data, Options options) {
				return new Handlebars.SafeString (data.toLowerCase ());
			}
		});
		engine.registerHelper ("formatDate", new Helper<Date>() {
			public CharSequence apply (Date date, Options options) {
				String format 	= options.param (0, Lang.UTC_DATE_FORMAT);
				return new Handlebars.SafeString (Lang.toString (date, format));
			}
		});
		engine.registerHelper ("now", new Helper<String>() {
			public CharSequence apply (String format, Options options) {
				if (Lang.isNullOrEmpty (format)) {
					format = Lang.UTC_DATE_FORMAT;
				};
				return new Handlebars.SafeString (Lang.toString (new Date (), format));
			}
		});
		engine.registerHelper ("set", new Helper<String>() {
			public CharSequence apply (String key, Options options) throws IOException {
				String value 	= options.param (0);
				if (value != null) {
					options.context.data (key, value);
				} 
				return options.fn ();
			}
		});
		
		// TODO: Numbers & Math helpers
		
		engine.registerHelper ("eq", new Helper<Object>() {
			public CharSequence apply (Object right, Options options) throws IOException {
				Object left = options.param (0);
				if (right == null && left == null) {
					return options.fn ();
				} else {
					if (right == null) {
						return options.inverse ();
					}
			        return right.equals (left) ? options.fn () : options.inverse ();
				}
			}
		});
		engine.registerHelper ("neq", new Helper<Object>() {
			public CharSequence apply (Object right, Options options) throws IOException {
				Object left = options.param (0);
				if (right == null && left == null) {
					return options.inverse ();
				} else {
					if (right == null) {
						return options.fn ();
					}
			        return !right.equals (left) ? options.fn () : options.inverse ();
				}
			}
		});
		engine.registerHelper ("lt", new Helper<Object>() {
			public CharSequence apply (Object right, Options options) throws IOException {
				Object left = options.param (0);
				if (right == null || left == null) {
					return options.inverse ();
				} else {
					Double dRight = null;
					try {
						dRight = Double.valueOf (String.valueOf (right));
					} catch (NumberFormatException nfex) {
						return options.inverse ();
					}
					Double dLeft = null;
					try {
						dLeft = Double.valueOf (String.valueOf (left));
					} catch (NumberFormatException nfex) {
						return options.inverse ();
					}
					return dLeft < dRight ? options.fn () : options.inverse ();
				}
			}
		});
		engine.registerHelper ("lte", new Helper<Object>() {
			public CharSequence apply (Object right, Options options) throws IOException {
				Object left = options.param (0);
				if (right == null || left == null) {
					return options.inverse ();
				} else {
					Double dRight = null;
					try {
						dRight = Double.valueOf (String.valueOf (right));
					} catch (NumberFormatException nfex) {
						return options.inverse ();
					}
					Double dLeft = null;
					try {
						dLeft = Double.valueOf (String.valueOf (left));
					} catch (NumberFormatException nfex) {
						return options.inverse ();
					}
					return dLeft <= dRight ? options.fn () : options.inverse ();
				}
			}
		});
		engine.registerHelper ("gt", new Helper<Object>() {
			public CharSequence apply (Object right, Options options) throws IOException {
				Object left = options.param (0);
				if (right == null || left == null) {
					return options.inverse ();
				} else {
					Double dRight = null;
					try {
						dRight = Double.valueOf (String.valueOf (right));
					} catch (NumberFormatException nfex) {
						return options.inverse ();
					}
					Double dLeft = null;
					try {
						dLeft = Double.valueOf (String.valueOf (left));
					} catch (NumberFormatException nfex) {
						return options.inverse ();
					}
					return dLeft > dRight ? options.fn () : options.inverse ();
				}
			}
		});
		engine.registerHelper ("gte", new Helper<Object>() {
			public CharSequence apply (Object right, Options options) throws IOException {
				Object left = options.param (0);
				if (right == null || left == null) {
					return options.inverse ();
				} else {
					Double dRight = null;
					try {
						dRight = Double.valueOf (String.valueOf (right));
					} catch (NumberFormatException nfex) {
						return options.inverse ();
					}
					Double dLeft = null;
					try {
						dLeft = Double.valueOf (String.valueOf (left));
					} catch (NumberFormatException nfex) {
						return options.inverse ();
					}
					return dLeft >= dRight ? options.fn () : options.inverse ();
				}
			}
		});
	}
	
	@Override
	public void write (ApiResource template, Map<String, Object> model, Writer writer)
			throws TemplateEngineException {
		
		final String uuid = template.path ();
		
		CachedTemplate cTemplate = templates.get (uuid);
		
		if (cTemplate == null || cTemplate.timestamp < template.timestamp ().getTime ()) {
			cTemplate = new CachedTemplate ();
			cTemplate.timestamp = template.timestamp ().getTime ();
			
			TemplateSource source = new TemplateSource () {
				@Override
				public long lastModified () {
					return template.timestamp ().getTime ();
				}
				@Override
				public String filename () {
					return uuid;
				}
				@Override
				public String content () throws IOException {
					
					InputStream is = null;
					
					try {
						is = template.toInput ();
						return IOUtils.toString (is);
					} finally {
						IOUtils.closeQuietly (is);
					}
				}
			};
			
			try {
				cTemplate.template = engine.compile (source);
			} catch (IOException e) {
				throw new TemplateEngineException (e.getMessage (), e);
			}
			
			templates.put (uuid, cTemplate);
			
		}
		
		try {
			cTemplate.template.apply (model, writer);
		} catch (Exception e) {
			throw new TemplateEngineException (e.getMessage (), e);
		}
		
	}
	
	class CachedTemplate {
		Template 	template;
		long		timestamp;
	}

}

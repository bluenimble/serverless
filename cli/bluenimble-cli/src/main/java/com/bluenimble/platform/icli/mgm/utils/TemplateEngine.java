package com.bluenimble.platform.icli.mgm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.TemplateSource;

public class TemplateEngine {
	
	protected static final Map<String, CachedTemplate> templates = new ConcurrentHashMap<String, CachedTemplate> ();
	
	private static Handlebars Engine = new Handlebars ();
	static {
		
		Engine.startDelimiter ("[[");
		Engine.endDelimiter ("]]");
		
		Engine.registerHelper ("json", new Helper<JsonObject>() {
			public CharSequence apply (JsonObject data, Options options) {
				return new Handlebars.SafeString (data.toString ());
			}
		});
		Engine.registerHelper ("truncate", new Helper<String>() {
			public CharSequence apply (String data, Options options) {
				Integer from 	= options.param (0, 0);
				Integer to 		= options.param (1, data.length ());
				return new Handlebars.SafeString (data.substring (from, to));
			}
		});
		Engine.registerHelper ("uppercase", new Helper<String>() {
			public CharSequence apply (String data, Options options) {
				return new Handlebars.SafeString (data.toUpperCase ());
			}
		});
		Engine.registerHelper ("lowercase", new Helper<String>() {
			public CharSequence apply (String data, Options options) {
				return new Handlebars.SafeString (data.toLowerCase ());
			}
		});
		Engine.registerHelper ("formatDate", new Helper<Date>() {
			public CharSequence apply (Date date, Options options) {
				String format 	= options.param (0, Lang.UTC_DATE_FORMAT);
				return new Handlebars.SafeString (Lang.toString (date, format));
			}
		});
		Engine.registerHelper ("now", new Helper<String>() {
			public CharSequence apply (String format, Options options) {
				if (Lang.isNullOrEmpty (format)) {
					format = Lang.UTC_DATE_FORMAT;
				};
				return new Handlebars.SafeString (Lang.toString (new Date (), format));
			}
		});
		Engine.registerHelper ("set", new Helper<String>() {
			public CharSequence apply (String key, Options options) throws IOException {
				String value 	= options.param (0);
				if (value != null) {
					options.context.data (key, value);
				} 
				return options.fn ();
			}
		});
		
		// Numbers helpers
		
		Engine.registerHelper ("eq", new Helper<Object>() {
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
		Engine.registerHelper ("neq", new Helper<Object>() {
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
		Engine.registerHelper ("lt", new Helper<Object>() {
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
		Engine.registerHelper ("lte", new Helper<Object>() {
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
		Engine.registerHelper ("gt", new Helper<Object>() {
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
		Engine.registerHelper ("gte", new Helper<Object>() {
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
		Engine.registerHelper ("has", new Helper<JsonObject>() {
			public CharSequence apply (JsonObject target, Options options) throws IOException {
				Object key = options.param (0);
				if (target == null || key == null) {
					return options.inverse ();
				} 
		        return target.containsKey (key) ? options.fn () : options.inverse ();
			}
		});
		Engine.registerHelper ("hasnt", new Helper<JsonObject>() {
			public CharSequence apply (JsonObject target, Options options) throws IOException {
				Object key = options.param (0);
				if (key == null) {
					return options.fn ();
				} 
				if (target == null) {
					return key != null ? options.fn () : options.inverse ();
				} 
		        return target.containsKey (key) ? options.inverse () : options.fn ();
			}
		});
		
	}

	public static String apply (String template, JsonObject data) throws Exception {
		return Engine.compileInline (template).apply (data);
	}
	
	public static void apply (File template, JsonObject data, Writer writer) throws Exception {
		template (template).apply (data, writer);
	}
	
	private static Template template (File template) throws Exception {
		
		String uuid = template.getAbsolutePath ();
		
		CachedTemplate cTemplate = templates.get (uuid);
		
		if (cTemplate != null && cTemplate.timestamp >= template.lastModified ()) {
			return cTemplate.template;
		}
		
		cTemplate = new CachedTemplate ();
		cTemplate.timestamp = template.lastModified ();
		
		TemplateSource source = new TemplateSource () {
			@Override
			public long lastModified () {
				return template.lastModified ();
			}
			@Override
			public String filename () {
				return uuid;
			}
			@Override
			public String content () throws IOException {
				
				InputStream is = null;
				
				try {
					is = new FileInputStream (template);
					return IOUtils.toString (is);
				} finally {
					IOUtils.closeQuietly (is);
				}
			}
		};
		
		cTemplate.template = Engine.compile (source);
		
		templates.put (uuid, cTemplate);
		
		return cTemplate.template;
		
	}
	
	static class CachedTemplate {
		Template 	template;
		long		timestamp;
	}
	
	public static void main (String[] args) throws Exception {
		
		System.out.println (
			apply ("hellow\\[[alpha]]", (JsonObject)new JsonObject ().set ("alpha", "Antoni"))
		);	
		
	}
	
}

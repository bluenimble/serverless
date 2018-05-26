package com.bluenimble.platform.icli.mgm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.IOUtils;
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

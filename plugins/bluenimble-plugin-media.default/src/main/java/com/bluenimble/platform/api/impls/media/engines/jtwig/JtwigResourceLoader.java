package com.bluenimble.platform.api.impls.media.engines.jtwig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import org.jtwig.resource.loader.ResourceLoader;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.google.common.base.Optional;

public class JtwigResourceLoader implements ResourceLoader {

	//private static final String Templates 				= "templates";

	private static final String TemplateExtension 		= "templateExtension";
	
	private static final String DotTpl 					= ".tpl";
	
	private Api 		api;
	//private String 		templatesPath;
	private String 		templateExt;
	
	public JtwigResourceLoader (Plugin plugin, Api api) {
		this.api 			= api;
		JsonObject feature 	= Json.getObject (api.getFeatures (), plugin.getNamespace ());
		/*
		templatesPath 		= Json.getString (feature, Templates, Templates);
		if (templatesPath.startsWith (Lang.SLASH)) {
			templatesPath = templatesPath.substring (1);
		}
		if (templatesPath.endsWith (Lang.SLASH)) {
			templatesPath = templatesPath.substring (0, templatesPath.length () - 1);
		}
		*/
		templateExt 		= Json.getString (feature, TemplateExtension, DotTpl);
	}
	
	@Override
	public boolean exists (String name) {
		if (!name.endsWith (templateExt)) {
			name = name + templateExt;
		}
		
		String fileName = /*templatesPath + Lang.SLASH +*/ name;

		ApiResource resource;
		try {
			resource = api.getResourcesManager ().get (Lang.split (fileName, Lang.SLASH));
		} catch (ApiResourcesManagerException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
		return resource != null;
	}

	@Override
	public Optional<Charset> getCharset (String uri) {
		return Optional.of (Charset.forName (Encodings.UTF8));
	}

	@Override
	public InputStream load (String name) {
		if (!name.endsWith (templateExt)) {
			name = name + templateExt;
		}
		
		String fileName = /*templatesPath + Lang.SLASH +*/ name;

		ApiResource resource;
		try {
			resource = api.getResourcesManager ().get (Lang.split (fileName, Lang.SLASH));
		} catch (ApiResourcesManagerException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
		if (resource == null) {
			return null;
		}
		try {
			return resource.toInput ();
		} catch (IOException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}

	@Override
	public Optional<URL> toUrl (String uri) {
		return null;
	}

}

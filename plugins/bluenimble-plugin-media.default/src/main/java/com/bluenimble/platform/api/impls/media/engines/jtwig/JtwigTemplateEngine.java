package com.bluenimble.platform.api.impls.media.engines.jtwig;

import java.nio.charset.Charset;

import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.DefaultEnvironmentConfiguration;
import org.jtwig.environment.Environment;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.environment.EnvironmentFactory;
import org.jtwig.resource.loader.ResourceLoader;
import org.jtwig.resource.loader.TypedResourceLoader;
import org.jtwig.resource.reference.ResourceReference;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngine;
import com.bluenimble.platform.api.impls.media.engines.TemplateEngineException;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.api.validation.impls.ValidationUtils;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.plugins.media.MediaPlugin;

public class JtwigTemplateEngine implements TemplateEngine {

	private static final long serialVersionUID = 3618016064363515328L;
	
	private static final String StartCode 		= "startCode";
	private static final String EndCode 		= "endCode";
	private static final String StartOutput 	= "startOutput";
	private static final String EndOutput 		= "endOutput";
	private static final String StartComment 	= "startComment";
	private static final String EndComment 		= "endComment";
	
	private static final String ValueGuesser 	= "valueGuesser";
	
	private ValueGuesser ValueGuesserInstance 	= new ValueGuesser ();

	interface Defaults {
		interface Start {
			String Code 	= "{%";
			String Output 	= "{{";
			String Comment 	= "{#";
		}
		interface End {
			String Code 	= "%}";
			String Output 	= "}}";
			String Comment 	= "#}";
		}
	}
	
	private static final String ResourceType 			= "resource";
	
	private static final EnvironmentFactory EnvironmentFactory = new EnvironmentFactory ();
	
	private ApiServiceValidator validator;

	private Environment environment;
	private Api 		api;
	private JsonObject 	config;
	
	public JtwigTemplateEngine (MediaPlugin plugin, Api api) {
		this.api = api;
		config 	= (JsonObject)Json.find (api.getFeatures (), plugin.getNamespace (), MediaPlugin.JtwigEngine);
		
		ResourceLoader loader = new JtwigResourceLoader (plugin, api);
		
		EnvironmentConfiguration configuration = 
				new EnvironmentConfigurationBuilder (new DefaultEnvironmentConfiguration ())
			.parser()
	            .syntax()
	                .withStartCode (Json.getString (config, StartCode, Defaults.Start.Code))
	                .withEndCode (Json.getString (config, EndCode, Defaults.End.Code))
	                .withStartOutput (Json.getString (config, StartOutput, Defaults.Start.Output))
	                .withEndOutput (Json.getString (config, EndOutput, Defaults.End.Output))
	                .withStartComment (Json.getString (config, StartComment, Defaults.Start.Comment))
	                .withEndComment (Json.getString (config, EndComment, Defaults.End.Comment))
	            .and ()
	         .and ()	
			.resources ().resourceLoaders ().add (new TypedResourceLoader (ResourceType, loader)).and ()
			.withDefaultInputCharset (Charset.forName (Encodings.UTF8))
	        .and ()
	    .build ();
		
		environment = EnvironmentFactory.create (configuration);
		
		validator = plugin.server ().getServiceValidator ();
		
	}
		
	@Override
	public void write (ApiConsumer consumer, ApiRequest request, ApiResponse response, ApiOutput output,
			ApiResource template, JsonObject mediaSpec) throws TemplateEngineException {
		
		// Resource
		ResourceReference resource = new ResourceReference (ResourceType, template.path ());

		// Template
		JtwigTemplate jtwigTemplate = new JtwigTemplate  (environment, resource);

		// Model
		JsonObject vars = new JsonObject ();
		
		vars.set (Json.getString (config, I18n, I18n), api.i18n (request.getLang ()));

		vars.set (Json.getString (config, Request, Request), request.toJson ());
		if (consumer != null) {
			vars.set (Json.getString (config, Consumer, Consumer), consumer.toJson ());
		}
		
		if (output != null) {
			vars.set (Json.getString (config, Output, Output), output.data ());
			vars.set (Json.getString (config, Meta, Meta), output.meta ());
		}
		vars.set (Json.getString (config, Error, Error), response.getError ());
		
		vars.set (Json.getString (config, ValueGuesser, ValueGuesser), ValueGuesserInstance);
		
		@SuppressWarnings("unchecked")
		JtwigModel model = JtwigModel.newModel (vars);

		// render
		try {
			jtwigTemplate.render (model, response.toOutput ());
		} catch (Exception e) {
			throw new TemplateEngineException (e.getMessage (), e);
		}
		
	}
	
	public class ValueGuesser {
		public Object guess (String name, JsonObject spec) {
			return ValidationUtils.guessValue (validator, name, spec);
		}
	}

}

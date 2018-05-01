package com.bluenimble.platform.remote.impls;

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.http.utils.ContentTypes;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.remote.Serializer;
import com.bluenimble.platform.remote.impls.serializers.JsonSerializer;
import com.bluenimble.platform.remote.impls.serializers.StreamSerializer;
import com.bluenimble.platform.remote.impls.serializers.TextSerializer;
import com.bluenimble.platform.remote.impls.serializers.XmlSerializer;
import com.bluenimble.platform.templating.ExpressionCompiler;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

import okhttp3.MediaType;

public abstract class AbstractRemote implements Remote {

	private static final long serialVersionUID = -4291878291944024539L;

	protected static final ExpressionCompiler ECompiler = new DefaultExpressionCompiler ();
	
	protected static final Map<Serializer.Name, Serializer> Serializers = new HashMap<Serializer.Name, Serializer> ();
	static {
		Serializers.put (Serializer.Name.text, 		new TextSerializer ());
		Serializers.put (Serializer.Name.json, 		new JsonSerializer ());
		Serializers.put (Serializer.Name.stream, 	new StreamSerializer ());
		Serializers.put (Serializer.Name.xml, 		new XmlSerializer ());
	}
	
	protected static final Map<String, MediaType> MediaTypes 	= new HashMap<String, MediaType> ();
	static {
		// media types
		MediaTypes.put (ContentTypes.Json, MediaType.parse (ContentTypes.Json));
		MediaTypes.put (Serializer.Name.json.name (), MediaTypes.get (ContentTypes.Json));
		MediaTypes.put (ContentTypes.FormUrlEncoded, MediaType.parse (ContentTypes.FormUrlEncoded));
		MediaTypes.put (ContentTypes.Multipart, MediaType.parse (ContentTypes.Multipart));
		MediaTypes.put (ContentTypes.Text, MediaType.parse (ContentTypes.Text));
	}
	
	protected interface Signers {
		String Bnb 		= "bnb";
		String OAuth 	= "oauth";
		String Basic	= "basic";
	}
	
	protected JsonObject featureSpec;
	
}

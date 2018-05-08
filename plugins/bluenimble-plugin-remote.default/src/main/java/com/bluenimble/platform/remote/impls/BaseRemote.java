package com.bluenimble.platform.remote.impls;

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.remote.Serializer;
import com.bluenimble.platform.remote.impls.serializers.JsonSerializer;
import com.bluenimble.platform.remote.impls.serializers.StreamSerializer;
import com.bluenimble.platform.remote.impls.serializers.TextSerializer;
import com.bluenimble.platform.remote.impls.serializers.XmlSerializer;
import com.bluenimble.platform.templating.ExpressionCompiler;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public abstract class BaseRemote extends AbstractRemote {

	private static final long serialVersionUID = -4291878291944024539L;

	protected static final ExpressionCompiler ECompiler = new DefaultExpressionCompiler ();
	
	protected static final Map<Serializer.Name, Serializer> Serializers = new HashMap<Serializer.Name, Serializer> ();
	static {
		Serializers.put (Serializer.Name.text, 		new TextSerializer ());
		Serializers.put (Serializer.Name.json, 		new JsonSerializer ());
		Serializers.put (Serializer.Name.stream, 	new StreamSerializer ());
		Serializers.put (Serializer.Name.xml, 		new XmlSerializer ());
	}
	
}

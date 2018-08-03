package com.bluenimble.platform.plugins.inbound.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiRequestBodyReader;
import com.bluenimble.platform.json.JsonObject;

public class YamlApiRequestBodyReader implements ApiRequestBodyReader {

	private static final long serialVersionUID = -9161966870378744014L;

	@Override
	@SuppressWarnings("unchecked")
	public Object read (InputStream payload, String contentType) throws IOException {
		
		Yaml yaml = new Yaml ();
		
		try {
			return new JsonObject (yaml.loadAs (payload, Map.class), true);
		} catch (Exception ex) {
			throw new IOException (ex.getMessage (), ex);
		}
	}

	@Override
	public String [] mediaTypes () {
		return new String [] { ApiContentTypes.Yaml, "text/yaml", "text/x-yaml", "application/x-yaml", "text/vnd.yaml", "application/vnd.yaml" };
	}

}
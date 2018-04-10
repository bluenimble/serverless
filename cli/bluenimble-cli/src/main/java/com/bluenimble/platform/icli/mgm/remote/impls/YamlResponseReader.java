package com.bluenimble.platform.icli.mgm.remote.impls;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.cli.impls.YamlObject;
import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpMessageBodyPart;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.icli.mgm.remote.RemoteUtils;
import com.bluenimble.platform.icli.mgm.remote.ResponseReader;

public class YamlResponseReader implements ResponseReader {

	private static final long serialVersionUID = -8389432849467983282L;

	@Override
	public CommandResult read (Tool tool, String contentType, HttpResponse response) throws Exception {
		
		CommandResult dcr = new DefaultCommandResult (response.getStatus () < 400 ? CommandResult.OK : CommandResult.KO, Lang.BLANK);
		
		HttpMessageBody body = response.getBody ();
		if (body == null || body.count () == 0) {
			return dcr;
		}
		
		HttpMessageBodyPart part = body.get (0);
		if (part == null) {
			return dcr;
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		part.dump (out, Encodings.UTF8);
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		Yaml yaml = new Yaml ();
		
		String ys = new String (((ByteArrayOutputStream)out).toByteArray ());
			   ys = Lang.replace (ys, Lang.TAB, "  ");	
			   
		@SuppressWarnings("unchecked")
		Map<String, Object> map = yaml.loadAs (ys, Map.class);
		Object trace = null;
		if (response.getStatus () >= 400) {
			trace = map.get ("trace");
			map.remove ("trace");
		}
		
		if (trace != null && Lang.isDebugMode ()) {
			vars.put (RemoteUtils.RemoteResponseError, trace);
		}
		
		return new DefaultCommandResult ((response.getStatus () < 400) ? CommandResult.OK : CommandResult.KO, new YamlObject (map));
		
	}	
	
}

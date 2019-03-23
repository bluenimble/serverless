package com.bluenimble.platform.icli.mgm.remote.impls;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpMessageBodyPart;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.icli.mgm.commands.mgm.RemoteCommand;
import com.bluenimble.platform.icli.mgm.remote.RemoteUtils;
import com.bluenimble.platform.icli.mgm.remote.ResponseReader;
import com.bluenimble.platform.json.JsonObject;

public class JsonResponseReader implements ResponseReader {

	private static final long serialVersionUID = -8389432849467983282L;

	@Override
	public CommandResult read (Tool tool, String contentType, JsonObject responseSpec, HttpResponse response) throws Exception {
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
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
		
		JsonObject json = new JsonObject (new String (((ByteArrayOutputStream)out).toByteArray ()));
		
		String trace = null;
		if (response.getStatus () >= 400) {
			trace = json.getString ("trace");
			json.remove ("trace");
			if (trace != null && Lang.isDebugMode ()) {
				vars.put (RemoteUtils.RemoteResponseError, trace);
			}
			return new DefaultCommandResult (CommandResult.KO, json);
		} 
		
		if (responseSpec != null) {
			String command = Json.getString (responseSpec, RemoteCommand.Spec.response.Command);
			if (!Lang.isNullOrEmpty (command)) {
				String resultId = Lang.UUID (20);
				vars.put (resultId, json);
				tool.processCommand (command + " " + resultId, false);
				return null;
			}
		}
		
		return new DefaultCommandResult (CommandResult.OK, json);
		
	}	
	
}

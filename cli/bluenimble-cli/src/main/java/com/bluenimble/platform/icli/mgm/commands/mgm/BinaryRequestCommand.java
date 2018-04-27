package com.bluenimble.platform.icli.mgm.commands.mgm;

import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.impls.SimpleApiRequest;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.remote.binary.BinaryClient;
import com.bluenimble.platform.icli.mgm.remote.binary.ResponseCallback;
import com.bluenimble.platform.json.JsonObject;

public class BinaryRequestCommand extends AbstractCommand  {
	
	private static final long serialVersionUID = -6558253473588586347L;

	public BinaryRequestCommand () {
		super ("call", "call a service using the binary protocol. Example call host:port RequestSpecVariable");
	}
	
	@Override
	public CommandResult execute (final Tool tool, Map<String, CommandOption> options) throws CommandExecutionException {
		
		String hostAndSpec = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (Lang.isNullOrEmpty (hostAndSpec)) {
			return null;
		}
		
		String [] tokens = Lang.split (hostAndSpec, Lang.SPACE);
		
		if (tokens.length < 2) {
			return new DefaultCommandResult (CommandResult.KO, "Missing argument. call [host:port] [SpecVariable]");
		}
		
		int indexOfColon = tokens [0].indexOf (Lang.COLON);
		if (indexOfColon < 0) {
			return new DefaultCommandResult (CommandResult.KO, "Invalid [host:port] '" + tokens [0] + "' argument");
		}
		
		String hostAndPort 	= tokens [0].trim ();
		String vSpec 		= tokens [1].trim ();
		
		String 	host = hostAndPort.substring (0, indexOfColon);
		int 	port = 0;
		try {
			port = Integer.valueOf (hostAndPort.substring (indexOfColon + 1));
		} catch (Exception ex) {
			return new DefaultCommandResult (CommandResult.KO, "Invalid port number '" + hostAndPort.substring (indexOfColon + 1) + "'");
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		JsonObject oSpec = (JsonObject)vars.get (vSpec);
		
		tool.printer ().content ("Connecting to ...", host + ":" + port);
		
		BinaryClient client = new BinaryClient (host, port);
		
		try {
			client.connect ();
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		
		client.send (new SimpleApiRequest (oSpec), new ResponseCallback () {
			@Override
			public void onStatus (ApiResponse.Status status) {
				System.out.println ("Status: " + status);
			}
			@Override
			public void onHeaders (Map<String, Object> headers) {
				System.out.println ("Headers: " + headers);
			}
			@Override
			public void onChunk (byte [] bytes) {
				System.out.println ("Chunk: \n" + bytes);
			}
			@Override
			public void onFinish () {
				System.out.println ("Done!");
			}
		});
		
		return new DefaultCommandResult (CommandResult.KO, "");

	}
	
}

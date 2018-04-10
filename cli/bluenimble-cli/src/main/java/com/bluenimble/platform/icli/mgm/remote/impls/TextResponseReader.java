package com.bluenimble.platform.icli.mgm.remote.impls;

import java.io.ByteArrayOutputStream;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpMessageBodyPart;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.icli.mgm.remote.ResponseReader;

public class TextResponseReader implements ResponseReader {

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
		
		String content = new String (((ByteArrayOutputStream)out).toByteArray ());
		
		return new DefaultCommandResult ((response.getStatus () < 400) ? CommandResult.OK : CommandResult.KO, content);
	}	
	
}

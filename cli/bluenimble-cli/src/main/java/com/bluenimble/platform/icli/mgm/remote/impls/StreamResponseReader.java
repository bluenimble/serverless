package com.bluenimble.platform.icli.mgm.remote.impls;

import java.io.InputStream;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.http.HttpMessageBody;
import com.bluenimble.platform.http.HttpMessageBodyPart;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.icli.mgm.remote.ResponseReader;

public class StreamResponseReader implements ResponseReader {

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
		
		final InputStream stream = part.toInputStream ();
		
		return new DefaultCommandResult ((response.getStatus () < 400) ? CommandResult.OK : CommandResult.KO, new ApiStreamSource () {
			private static final long serialVersionUID = 620395671712463132L;
			@Override
			public InputStream stream () {
				return stream;
			}
			@Override
			public String name () {
				return part.getFileName ();
			}
			
			@Override
			public String id () {
				return part.getName ();
			}
			
			@Override
			public String contentType () {
				return contentType;
			}
			
			@Override
			public long length () {
				return 0;
			}
			
			@Override
			public void close () {
				IOUtils.closeQuietly (stream);
			}
		});
	}	
	
}

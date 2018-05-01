package com.bluenimble.platform.server.plugins.inbound.binary.netty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.impls.AbstractApiResponse;

import io.netty.channel.ChannelHandlerContext;

public class NettyResponse extends AbstractApiResponse {

	private static final long serialVersionUID = -2164934807466002434L;
	
	private ChannelHandlerContext 	context;
	
	private boolean					headersWritten;
	
	public NettyResponse (String id, ChannelHandlerContext context) {
		super (id);
		this.context = context;
	}

	@Override
	protected ApiResponse append (byte [] buff, int offset, int length) throws IOException {
		context.write (buff);
		return this;
	}

	@Override
	public OutputStream toOutput () throws IOException {
		throw new UnsupportedOperationException ("toOutput is unsupported by " + this.getClass ().getSimpleName ());
	}

	@Override
	public Writer toWriter () throws IOException {
		throw new UnsupportedOperationException ("toWriter is unsupported by " + this.getClass ().getSimpleName ());
	}

	@Override
	public void flushHeaders () {
		if (headersWritten) {
			return;
		}
		
		if (status != null) {
			context.write (status.getCode ());
			status = null;
		}
		
		if (headers == null || headers.isEmpty ()) {
			return;
		}
		
		context.write (headers);
		
		headersWritten = true;
	}

	@Override
	public void close () throws IOException {
		commit ();
	}

	@Override
	public void commit () {
		
	}

}
